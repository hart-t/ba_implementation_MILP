package models;

import com.gurobi.gurobi.*;

import enums.ModelType;
import io.JobDataInstance;
import utility.TEMatrix;

import interfaces.CompletionMethodInterface;
import interfaces.ModelInterface;
import interfaces.ModelSolutionInterface;
import solutionBuilder.BuildFlowSolution;

/**
 * Flow-based continuous time model for the Resource-Constrained Project Scheduling Problem (RCPSP).
 * This model uses Gurobi to solve the problem by defining variables, constraints, and an objective function.
 * It is designed to handle precedence relations, resource constraints, and flow conservation.
 * The model is based on the flow-based approach described in the literature.
 *
 * https://www.sciencedirect.com/science/article/pii/S0305054809003360
 *      flow based continuous
 *
 *      public final int numberJob;
 *      public final int horizon;
 *      public final List<Integer> jobNumSuccessors;
 *      public final List<List<Integer>> jobSuccessors;
 *      public final List<List<Integer>> jobPredecessors;
 *      public final List<Integer> jobDuration;
 *      public final List<List<Integer>> jobResource;
 *      public final List<Integer> resourceCapacity;
 * */

public class FlowBasedContinuousTimeModel implements ModelInterface {
    CompletionMethodInterface completionMethod;

    public FlowBasedContinuousTimeModel() {
        // Constructor
        this.completionMethod = new BuildFlowSolution();
    }

    @Override
    public CompletionMethodInterface getCompletionMethod() {
        return completionMethod;
    }

    public GRBModel completeModel(ModelSolutionInterface initialSolution, JobDataInstance data) {
        try {
            GRBModel model = initialSolution.getModel();

            // Cast to access the Variables
            GRBVar[] startingTimeVars;
            GRBVar[][] precedenceVars;
            GRBVar[][][] continuousFlowVars;
            int[] earliestStartTime;
            int[] latestStartTime;
            if (initialSolution instanceof FlowBasedContinuousTimeModelSolution) {
                startingTimeVars = ((FlowBasedContinuousTimeModelSolution) initialSolution)
                        .getStartingTimeVars();
                precedenceVars = ((FlowBasedContinuousTimeModelSolution) initialSolution)
                        .getPrecedenceVars();
                continuousFlowVars = ((FlowBasedContinuousTimeModelSolution) initialSolution)
                        .getContinuousFlowVars();
                earliestStartTime = ((FlowBasedContinuousTimeModelSolution) initialSolution)
                        .getEarliestLatestStartTimes()[0];
                latestStartTime = ((FlowBasedContinuousTimeModelSolution) initialSolution)
                        .getEarliestLatestStartTimes()[1];
            } else {
                throw new IllegalArgumentException("Expected DiscreteTimeModelSolution but got " + 
                                                 initialSolution.getClass().getSimpleName());
            }

            // generate TE matrix, This means that if there is a path from activity i to j in the precedence graph
            // (even if not directly), then (i,j) element TE.
            int[][] teMatrix = TEMatrix.computeTEMatrix(data.numberJob, data.jobSuccessors, data.jobDuration);

            // activity 0 acts as a resource source while activity acts as a resource sink.
            int[][] resourceDemands = new int[data.numberJob][data.jobResource.size()];
            for (int i = 0; i < data.numberJob; i++) {
                for (int k = 0; k < data.resourceCapacity.size(); k++) {
                    if (i == 0) {
                        resourceDemands[i][k] = data.resourceCapacity.get(k);
                    } else if (i == data.numberJob - 1) {
                        resourceDemands[i][k] = data.resourceCapacity.get(k);
                    } else {
                        resourceDemands[i][k] = data.jobResource.get(i).get(k);
                    }
                }
            }

            // Constraints (12) state that for two distinct activities, either i precedes j, or j precedes i, or i and j are
            // processed in parallel.
            for (int i = 0; i < data.numberJob; i++) {
                for (int j = i; j < data.numberJob; j++) {
                    if (i == j) continue; // Skip self-precedence
                    GRBLinExpr expr = new GRBLinExpr();
                    expr.addTerm(1, precedenceVars[i][j]);
                    expr.addTerm(1, precedenceVars[j][i]);
                    model.addConstr(expr, GRB.LESS_EQUAL, 1, "C12_precedence_mutual_exclusion_job_" + i + "_job_" + j);
                }
            }

            // Constraints (13) express the transitivity of the precedence relations.
            for (int i = 0; i < data.numberJob; i++) {
                for (int j = 0; j < data.numberJob; j++) {
                    if (i == j) continue;
                    for (int k = 0; k < data.numberJob; k++) {
                        if (i == k) continue;
                        if (k == j) continue;

                        GRBLinExpr expr1 = new GRBLinExpr();
                        GRBLinExpr expr2 = new GRBLinExpr();

                        expr1.addTerm(1, precedenceVars[i][k]);

                        expr2.addTerm(1, precedenceVars[i][j]);
                        expr2.addTerm(1, precedenceVars[j][k]);
                        expr2.addConstant(-1);

                        model.addConstr(expr1, GRB.GREATER_EQUAL, expr2, "C13_precedence_transitivity_job_" + i + "_job_" + j + "_job_" + k);
                    }
                }
            }

            // Constraints (14) are so-called disjunctive constraints linking the start time of i and j with respect to
            // variable xij.
            // p duration
            for (int i = 0; i < data.numberJob; i++) {
                for (int j = 0; j < data.numberJob; j++) {
                    if (i == j) continue;
                    // Mij is some large enough constant, which can be set to any valid upper bound for Si - Sj
                    // (e.g., Mij = ESi - LSj)
                    /*
                    if i calculate Mij like its described as ESi - LSj and i precedes j then the Mij is negative?
                    but the same discription said to set Mij as a large enough constant
                    */
                    //int Mij = earliestStartTime[i] - latestStartTime[j];
                    int Mij = latestStartTime[i] - earliestStartTime[j];
                    // latest start i -earliest start j
                    //Mij = data.horizon;
                    GRBLinExpr leftSide = new GRBLinExpr();
                    GRBLinExpr rightSide = new GRBLinExpr();

                    leftSide.addTerm(1, startingTimeVars[j]);
                    leftSide.addTerm(-1, startingTimeVars[i]);

                    rightSide.addConstant(-Mij);
                    rightSide.addTerm((data.jobDuration.get(i) + Mij), precedenceVars[i][j]);

                    model.addConstr(leftSide, GRB.GREATER_EQUAL, rightSide, "C14_disjunctive_timing_job_" + i + "_job_" + j);
                }
            }

            // Constraints (15) link flow variables and xij variables. If i precedes j, the maximum flow sent
            // from i to j is set to min{bik, bjk} while if i does not precede j the flow must be zero.
            for (int i = 0; i < data.numberJob - 1; i++) {
                for (int j = 1; j < data.numberJob; j++) {
                    if (i == j) continue;
                    if (precedenceVars[i][j] == null) continue;
                    for (int k = 0; k < data.resourceCapacity.size(); k++) {
                        GRBLinExpr expr1 = new GRBLinExpr();
                        GRBLinExpr expr2 = new GRBLinExpr();

                        expr1.addTerm(1, continuousFlowVars[i][j][k]);
                        int minResourceDemand = Math.min(resourceDemands[i][k], resourceDemands[j][k]);
                        expr2.addTerm(minResourceDemand, precedenceVars[i][j]);

                        model.addConstr(expr1, GRB.LESS_EQUAL, expr2, "C15_flow_precedence_link_job_" + i + "_job_" + j + "_resource_" + k);
                    }
                }
            }

            // Constraints (16), (17), (18) are resource flow conservation constraints.
            // the last job (supersink) has no outgoing flow, so it is not included in this loop.
            for (int i = 0; i < data.numberJob; i++) {
                for (int k = 0; k < data.resourceCapacity.size(); k++) {

                    GRBLinExpr expr1 = new GRBLinExpr();
                    GRBLinExpr expr2 = new GRBLinExpr();

                    for (int j = 0; j < data.numberJob; j++) {
                        if (i == j) continue; // Skip self-
                        expr1.addTerm(1, continuousFlowVars[i][j][k]);
                    }

                    expr2.addConstant(resourceDemands[i][k]);

                    model.addConstr(expr1, GRB.EQUAL, expr2, "C16_flow_conservation_outgoing_job_" + i + "_resource_" + k);
                }
            }

            // (17) i think there is a typo and it should be "for all j" instead of "for all i" like in C16
            // Job 0 does not have any incoming flow, so it is not included in this loop.
            for (int j = 0; j < data.numberJob; j++) {
                for (int k = 0; k < data.resourceCapacity.size(); k++) {

                    GRBLinExpr expr1 = new GRBLinExpr();
                    GRBLinExpr expr2 = new GRBLinExpr();

                    for (int i = 0; i < data.numberJob; i++) {
                    if (i == j) continue; // Skip self-
                        expr1.addTerm(1, continuousFlowVars[i][j][k]);
                    }

                    expr2.addConstant(resourceDemands[j][k]);

                    model.addConstr(expr1, GRB.EQUAL, expr2, "C17_flow_conservation_incoming_job_" + j + "_resource_" + k);
                }
            }

            // (18)
            for (int k = 0; k < data.resourceCapacity.size(); k++) {
                GRBLinExpr expr1 = new GRBLinExpr();
                GRBLinExpr expr2 = new GRBLinExpr();

                expr1.addTerm(1, continuousFlowVars[data.numberJob - 1][0][k]);
                expr2.addConstant(data.resourceCapacity.get(k));

                model.addConstr(expr1, GRB.EQUAL, expr2, "C18_supersink_supersource_flow_resource_" + k);
            }



            // Constraints (19), (20) set the preexisting precedence constraints.
            for (int i = 0; i < data.numberJob; i++) {
                for (int j = 0; j < data.numberJob; j++) {
                    if (i == j) continue;

                    GRBLinExpr expr1 = new GRBLinExpr();
                    GRBLinExpr expr2 = new GRBLinExpr();
                    GRBLinExpr expr3 = new GRBLinExpr();

                    expr1.addTerm(teMatrix[i][j], precedenceVars[i][j]);
                    expr2.addConstant(1);
                    expr3.addConstant(0);

                    if (teMatrix[i][j] == 1) {
                        model.addConstr(expr1, GRB.EQUAL, expr2, "C19_enforce_precedence_job_" + i + "_before_job_" + j);
                    }
                    if (teMatrix[i][j] == 0) {
                        model.addConstr(expr1, GRB.EQUAL, expr3, "C20_prohibit_precedence_job_" + i + "_before_job_" + j);
                    }
                }
            }

            // (21) non-negative constraint for flow variables
            for (int i = 0; i < data.numberJob; i++) {
                for (int j = 0; j < data.numberJob; j++) {
                    if (i == j) continue; // Skip self
                    for (int k = 0; k < data.resourceCapacity.size(); k++) {
                        GRBLinExpr expr1 = new GRBLinExpr();
                        GRBLinExpr expr2 = new GRBLinExpr();

                        expr1.addTerm(1, continuousFlowVars[i][j][k]);
                        expr2.addConstant(0);

                        model.addConstr(expr1, GRB.GREATER_EQUAL, expr2, "C21_flow_non_negative_job_" + i + "_job_" + j + "_resource_" + k);
                    }
                }
            }



            // (22) set dummy starter starting time at 0
            GRBLinExpr expr01 = new GRBLinExpr();
            GRBLinExpr expr02 = new GRBLinExpr();

            expr01.addTerm(1, startingTimeVars[0]);
            expr02.addConstant(0);

            model.addConstr(expr01, GRB.EQUAL, expr02, "C22_supersource_start_time_zero");

            // (23) Starting time of variable x is between its earliest and latest starting time
            for (int i = 0; i < data.numberJob; i++) {
                GRBLinExpr expr1 = new GRBLinExpr();
                GRBLinExpr expr2 = new GRBLinExpr();
                GRBLinExpr expr3 = new GRBLinExpr();

                expr1.addTerm(1, startingTimeVars[i]);
                expr2.addConstant(earliestStartTime[i]);
                expr3.addConstant(latestStartTime[i]);

                model.addConstr(expr2, GRB.LESS_EQUAL, expr1, "C23_earliest_start_time_bound_job_" + i);
                model.addConstr(expr1, GRB.LESS_EQUAL, expr3, "C23_latest_start_time_bound_job_" + i);
            }

            // Objective function: minimize the starting time of the last job
            // This is equivalent to minimizing the makespan of the schedule
            GRBLinExpr obj = new GRBLinExpr();
            obj.addTerm(1, startingTimeVars[data.numberJob - 1]);
            model.setObjective(obj, GRB.MINIMIZE);

            return model;
        } catch (GRBException e) {
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean usesDummyJobs() {
        // This model uses dummy jobs (supersource and supersink)
        return true;
    }

    public class FlowBasedContinuousTimeModelSolution implements ModelSolutionInterface {
        GRBVar[] startingTimeVars;
        GRBVar[][] precedenceVars;
        GRBVar[][][] continuousFlowVars;
        long timeToCreateVariables;

        GRBModel model;
        int[][] earliestLatestStartTimes;

        public FlowBasedContinuousTimeModelSolution(GRBVar[] startingTimeVars, GRBVar[][] precedenceVars,
                        GRBVar[][][] continuousFlowVars, GRBModel model, int[][] earliestLatestStartTimes, long timeToCreateVariables) {

            this.startingTimeVars = startingTimeVars;
            this.precedenceVars = precedenceVars;
            this.continuousFlowVars = continuousFlowVars;
            this.model = model;
            this.earliestLatestStartTimes = earliestLatestStartTimes;
            this.timeToCreateVariables = timeToCreateVariables;
        }

        @Override
        public GRBModel getModel() {
            return model;
        }

        public int[][] getEarliestLatestStartTimes() {
            return earliestLatestStartTimes;
        }

        public GRBVar[] getStartingTimeVars() {
            return startingTimeVars;
        }

        public GRBVar[][] getPrecedenceVars() {
            return precedenceVars;
        }

        public GRBVar[][][] getContinuousFlowVars() {
            return continuousFlowVars;
        }

        @Override
        public ModelType getModelType() {
            return ModelType.FLOW;
        }

        @Override
        public long getTimeToCreateVariables() {
            return timeToCreateVariables;
        }
    }

    @Override
    public int[][] getStartAndFinishTimes(GRBModel model, JobDataInstance data) {
        try {
            int[] startTimes = new int[data.numberJob];
            int[] finishTimes = new int[data.numberJob];
            
            // Initialize start times to 0
            for (int i = 0; i < data.numberJob; i++) {
                startTimes[i] = 0;
            }
            
            // Extract start times from continuous variables
            for (GRBVar var : model.getVars()) {
                String varName = var.get(GRB.StringAttr.VarName);
                if (varName.startsWith("startingTime[")) {
                    // Extract job index from variable name "startingTime[i]"
                    int jobIndex = Integer.parseInt(varName.substring("startingTime[".length(), varName.indexOf(']')));
                    double startTime = var.get(GRB.DoubleAttr.X);
                    
                    // Round to nearest integer for discrete time representation
                    if (Math.abs(startTime - Math.round(startTime)) < 0.01) {
                        startTimes[jobIndex] = (int) Math.round(startTime);
                    } else {
                        startTimes[jobIndex] = (int) Math.ceil(startTime);
                    }
                }
            }
            
            // Calculate finish times: startTime + duration
            for (int i = 0; i < data.numberJob; i++) {
                finishTimes[i] = startTimes[i] + data.jobDuration.get(i);
            }
            
            return new int[][]{startTimes, finishTimes};
            
        } catch (GRBException e) {
            e.printStackTrace();
        }
        return null;
    }
}