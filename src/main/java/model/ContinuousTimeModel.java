package model;

import com.gurobi.gurobi.*;

import java.util.*;

/*
        https://www.sciencedirect.com/science/article/pii/S0305054809003360
        flow based continuous

        public final int numberJob;
        public final int horizon;
        public final List<Integer> jobNumSuccessors;
        public final List<List<Integer>> jobSuccessors;
        public final List<List<Integer>> jobPredecessors;
        public final List<Integer> jobDuration;
        public final List<List<Integer>> jobResource;
        public final List<Integer> resourceCapacity;
 */

public class ContinuousTimeModel {
    public record ScheduleResult(List<Double> start, List<Double> finish) {
    }

    public static void gurobiRcpspJ30(String file) throws Exception {
        // Create FileReader instance and get the data
        FileReader fileReader = new FileReader();
        FileReader.JobData data = fileReader.dataRead(file);

        // T=100 like its recommended for J30 instances (time to solve)
        final int T = 100;

        // Initialize the Gurobi environment and model
        GRBEnv env = new GRBEnv();
        GRBModel model = new GRBModel(env);

        //add starting time variables
        GRBVar startingTimeVars[] =new GRBVar[data.numberJob];
        for (int i = 0; i < data.numberJob; i++) {
            startingTimeVars[i] = model.addVar(0.0, data.horizon, 0.0, GRB.CONTINUOUS, "startingTime[" + i + "]");
        }

        //indicate whether activity i is processed before activity j
        GRBVar precedenceVars[][] =new GRBVar[data.numberJob][data.numberJob];
        for (int i = 0; i < data.numberJob; i++) {
            for (int j = 0; j < data.numberJob; j++) {
                precedenceVars[i][j] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "[" + i + "] precedes [" + j + "]");
            }
        }

        //Finally, continuous flow variables are introduced to denote the quantity of resource k that is transferred
        // from activity i (at the end of its processing) to activity j (at the start of its processing).
        GRBVar continuousFlowVars[][][] =new GRBVar[data.numberJob][data.numberJob][data.resourceCapacity.size()];
        for (int i = 0; i < data.numberJob; i++) {
            for (int j = 0; j < data.numberJob; j++) {
                for (int k = 0; k < data.resourceCapacity.size(); k++) {
                    continuousFlowVars[i][j][k] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY,
                            "quantity of resource " + k + "transferred from " + i + " to " + j);
                }
            }
        }

        //activity 0 acts as a resource source while activity acts as a resource sink.
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

        //Constraints (12) state that for two distinct activities, either i precedes j, or j precedes i, or i and j are
        //processed in parallel.
        for (int i = 0; i < data.numberJob; i++) {
            for (int j = i; j < data.numberJob; j++) {
                GRBLinExpr expr = new GRBLinExpr();
                expr.addTerm(1, precedenceVars[i][j]);
                expr.addTerm(1, precedenceVars[j][i]);
                model.addConstr(expr, GRB.LESS_EQUAL, 1, i + " and " + j + " cannot both precede each " +
                        "other (C12)");
            }
        }

        //Constraints (13) express the transitivity of the precedence relations.
        for (int i = 0; i < data.numberJob; i++) {
            for (int j = 0; j < data.numberJob; j++) {
                for (int k = 0; k < data.numberJob; k++) {
                    GRBLinExpr expr1 = new GRBLinExpr();
                    GRBLinExpr expr2 = new GRBLinExpr();

                    expr1.addTerm(1, precedenceVars[i][k]);

                    expr2.addTerm(1, precedenceVars[i][j]);
                    expr2.addTerm(1, precedenceVars[j][k]);
                    expr2.addConstant(-1);

                    model.addConstr(expr1, GRB.GREATER_EQUAL, expr2, expr1.toString() + "greater equal to " +
                            expr2.toString() + " (C13)");
                }
            }
        }

        //Constraints (14) are so-called disjunctive constraints linking the start time of i and j with respect to
        //variable xij.
        //p duration
        for (int i = 0; i < data.numberJob; i++) {
            for (int j = 0; j < data.numberJob; j++) {
                GRBLinExpr expr1 = new GRBLinExpr();
                GRBLinExpr expr2 = new GRBLinExpr();

                expr1.addTerm(1, startingTimeVars[j]);
                expr1.addTerm(-1, startingTimeVars[i]);

                expr2.addConstant(-data.horizon);
                expr2.addTerm((data.jobDuration.get(i) + data.horizon), precedenceVars[i][j]);

                model.addConstr(expr1, GRB.GREATER_EQUAL, expr2, expr1.toString() + "greater equal to " +
                        expr2.toString() + " (C14)");
            }
        }

        //Constraints (15) link flow variables and xij variables. If i precedes j, the maximum flow sent
        //from i to j is set to min{bik, bjk} while if i does not precede j the flow must be zero.
        for (int i = 0; i < data.numberJob - 1; i++) {
            for (int j = 1; j < data.numberJob; j++) {
                for (int k = 0; k < data.resourceCapacity.size(); k++) {
                    GRBLinExpr expr1 = new GRBLinExpr();
                    GRBLinExpr expr2 = new GRBLinExpr();

                    expr1.addTerm(1, continuousFlowVars[i][j][k]);
                    int minResourceDemand = Math.min(resourceDemands[i][k], resourceDemands[j][k]);
                    expr2.addTerm(minResourceDemand, precedenceVars[i][j]);

                    model.addConstr(expr1, GRB.LESS_EQUAL, expr2, expr1.toString() + "less equal to " +
                            expr2.toString() + " (C15)");
                }
            }
        }

        //Constraints (16), (17), (18) are resource flow conservation constraints.
        for (int i = 0; i < data.numberJob; i++) {
            for (int k = 0; k < data.resourceCapacity.size(); k++) {

                GRBLinExpr expr1 = new GRBLinExpr();
                GRBLinExpr expr2 = new GRBLinExpr();

                for (int j = 0; j < data.numberJob; j++) {
                    expr1.addTerm(1, continuousFlowVars[i][j][k]);
                }

                expr2.addConstant(resourceDemands[i][k]);

                model.addConstr(expr1, GRB.EQUAL, expr2, expr1.toString() + " equals " +
                        expr2.toString() + " (C16)");
            }
        }

        //(17) i think there is a typo and it should be "for all j" instead of "for all i" like in C16
        //TODO
        for (int j = 0; j < data.numberJob; j++) {
            for (int k = 0; k < data.resourceCapacity.size(); k++) {

                GRBLinExpr expr1 = new GRBLinExpr();
                GRBLinExpr expr2 = new GRBLinExpr();

                for (int i = 0; i < data.numberJob; i++) {
                    expr1.addTerm(1, continuousFlowVars[i][j][k]);
                }

                expr2.addConstant(resourceDemands[j][k]);

                model.addConstr(expr1, GRB.EQUAL, expr2, expr1.toString() + " equals " +
                        expr2.toString() + " (C17)");
            }
        }

        //(18)
        for (int k = 0; k < data.resourceCapacity.size(); k++) {
            GRBLinExpr expr1 = new GRBLinExpr();
            GRBLinExpr expr2 = new GRBLinExpr();

            expr1.addTerm(1, continuousFlowVars[data.numberJob - 1][0][k]);
            expr2.addConstant(data.resourceCapacity.get(k));

            model.addConstr(expr1, GRB.EQUAL, expr2, expr1.toString() + " equals " +
                    expr2.toString() + " (C18)");
        }

        //generate TE matrix, This means that if there is a path from activity i to j in the precedence graph
        //(even if not directly), then (i,j) element TE.
        int[][] TE = computeTEMatrix(data.numberJob, data.jobPredecessors);
        for (int[] row : TE) {
            System.out.println(Arrays.toString(row));
        }

        //Constraints (19), (20) set the preexisting precedence constraints.
        for (int i = 0; i < data.numberJob; i++) {
            for (int j = 0; j < data.numberJob; j++) {
                GRBLinExpr expr1 = new GRBLinExpr();
                GRBLinExpr expr2 = new GRBLinExpr();
                GRBLinExpr expr3 = new GRBLinExpr();

                expr1.addTerm(1, precedenceVars[i][j]);
                expr2.addConstant(1);
                expr3.addConstant(0);

                model.addConstr(expr1, GRB.EQUAL, expr2, expr1.toString() + " equals " +
                        expr2.toString() + " (C19)");
                model.addConstr(expr1, GRB.EQUAL, expr3, expr1.toString() + " equals " +
                        expr3.toString() + " (C20)");
            }
        }

        //(21) non-negative constraint for flow variables
        for (int i = 0; i < data.numberJob; i++) {
            for (int j = 0; j < data.numberJob; j++) {
                for (int k = 0; k < data.resourceCapacity.size(); k++) {
                    GRBLinExpr expr1 = new GRBLinExpr();
                    GRBLinExpr expr2 = new GRBLinExpr();

                    expr1.addTerm(1, continuousFlowVars[i][j][k]);
                    expr2.addConstant(0);

                    model.addConstr(expr1, GRB.GREATER_EQUAL, expr2, expr1.toString() + "non negative (C21)");
                }
            }
        }



        //(22) set dummy starter starting time at 0
        GRBLinExpr expr1 = new GRBLinExpr();
        GRBLinExpr expr2 = new GRBLinExpr();

        expr1.addTerm(1, startingTimeVars[0]);
        expr2.addConstant(0);

        model.addConstr(expr1, GRB.EQUAL, expr2, expr1.toString() + "starting time " + expr2.toString()
                        + "(C22)");

        //(23) Starting time of variable x is between its earliest and latest starting time




        //GRBVar[][] x = new GRBVar[data.numberJob][T];
        //fillWithCVariables(model, startingTimeVars, precedenceVars, continuousFlowVars, resourceDemands);

        // Write model to file and optimize
        model.write("linear_model.lp");
        model.optimize();


        // Clean up Gurobi model and environment
        model.dispose();
        env.dispose();
    }

    private static void fillWithCVariables(GRBModel model, GRBVar[] startingTimeVars, GRBVar[][] precedenceVars,
                                           GRBVar[][][] continuousFlowVars, int[][] resourceDemands) {



    }


    //TODO testen!!
    public static int[][] computeTEMatrix(int jobCount, List<List<Integer>> jobPredecessors) {
        int[][] teMatrix = new int[jobCount][jobCount];

        for (int j = 0; j < jobCount; j++) {
            Set<Integer> allPredecessors = new HashSet<>();
            collectAllPredecessors(j, allPredecessors, jobPredecessors);
            for (int i : allPredecessors) {
                teMatrix[i][j] = 1;
            }
        }

        return teMatrix;
    }
    //TODO testen!!
    private static void collectAllPredecessors(int job, Set<Integer> result, List<List<Integer>> jobPredecessors) {
        for (int pred : jobPredecessors.get(job)) {
            if (result.add(pred)) {
                collectAllPredecessors(pred, result, jobPredecessors);
            }
        }
    }

}
