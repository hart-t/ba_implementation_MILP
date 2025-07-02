package models;

import com.gurobi.gurobi.*;

import io.JobDataInstance;
import utility.DAGLongestPath;

import java.util.*;
import interfaces.ModelInterface;
import interfaces.ModelSolutionInterface;
import io.Result;

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

    public FlowBasedContinuousTimeModel() {}

    public Result solve(ModelSolutionInterface initialSolution, JobDataInstance data) {
        try {
            GRBEnv env = new GRBEnv();
            GRBModel model = new GRBModel(env);

            // Call the method to build the model
            //model = gurobiRcpspJ30(model, data);

            // Optimize the model
            model.optimize();

            // Extract results
            Map<Integer, Integer> startTimes = new HashMap<>();
            for (int i = 0; i < data.numberJob; i++) {
                startTimes.put(i, (int) model.getVarByName("startingTime[" + i + "]").get(GRB.DoubleAttr.X));
            }

            // Set the start times in the data instance
            data.setStartTimes(startTimes);

            // Dispose of the model and environment
            model.dispose();
            env.dispose();

            return null;

        } catch (GRBException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static GRBModel gurobiRcpspJ30(GRBModel model,
     JobDataInstance data ) throws Exception {

        // generate TE matrix, This means that if there is a path from activity i to j in the precedence graph
        // (even if not directly), then (i,j) element TE.
        int[][] teMatrix = computeTEMatrix(data.numberJob, data.jobSuccessors, data.jobDuration);

        /*for (int[] row : teMatrix) {
            System.out.println(Arrays.toString(row));
        }*/

        // 
        int[][] startTimes = generateEarliestAndLatestStartTimes(data.jobPredecessors, data.jobDuration, data.horizon);
        int[] earliestStartTime = startTimes[0];
        int[] latestStartTime = startTimes[1];

        // add starting time variables
        GRBVar[] startingTimeVars =new GRBVar[data.numberJob];
        for (int i = 0; i < data.numberJob; i++) {
            startingTimeVars[i] = model.addVar(0.0, data.horizon, 0.0, GRB.CONTINUOUS, "startingTime[" +
                    i + "]");
        }

        // indicate whether activity i is processed before activity j
        GRBVar[][] precedenceVars =new GRBVar[data.numberJob][data.numberJob];
        for (int i = 0; i < data.numberJob; i++) {
            for (int j = 0; j < data.numberJob; j++) {
                if (i == j) continue; // Skip self-precedence
                precedenceVars[i][j] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "[" + i +
                        "] precedes [" + j + "]");
            }
        }

        // Finally, continuous flow variables are introduced to denote the quantity of resource k that is transferred
        // from activity i (at the end of its processing) to activity j (at the start of its processing).
        GRBVar[][][] continuousFlowVars =new GRBVar[data.numberJob][data.numberJob][data.resourceCapacity.size()];
        for (int i = 0; i < data.numberJob; i++) {
            for (int j = 0; j < data.numberJob; j++) {
                if (i == j) continue; // Skip self-transfer
                for (int k = 0; k < data.resourceCapacity.size(); k++) {
                    continuousFlowVars[i][j][k] = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS,
                            "quantity of resource " + k + "transferred from " + i + " to " + j);
                }
            }
        }

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
                model.addConstr(expr, GRB.LESS_EQUAL, 1, i + " and " + j + " cannot both precede each " +
                        "other (C12)");
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

                    model.addConstr(expr1, GRB.GREATER_EQUAL, expr2, expr1.toString() + "greater equal to " +
                            expr2.toString() + " (C13)");
                }
            }
        }

        // Constraints (14) are so-called disjunctive constraints linking the start time of i and j with respect to
        // variable xij.
        // p duration
        for (int i = 0; i < data.numberJob; i++) {
            for (int j = 0; j < data.numberJob; j++) {
                if (i == j) continue;
                if (precedenceVars[i][j] == null) continue;
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
                GRBLinExpr expr1 = new GRBLinExpr();
                GRBLinExpr expr2 = new GRBLinExpr();

                expr1.addTerm(1, startingTimeVars[j]);
                expr1.addTerm(-1, startingTimeVars[i]);

                expr2.addConstant(-Mij);
                expr2.addTerm((data.jobDuration.get(i) + Mij), precedenceVars[i][j]);

                model.addConstr(expr1, GRB.GREATER_EQUAL, expr2, expr1 + "greater equal to " + expr2 + " (C14)");
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

                    model.addConstr(expr1, GRB.LESS_EQUAL, expr2, expr1 + "less equal to " + expr2 + " (C15)");
                }
            }
        }

        // Constraints (16), (17), (18) are resource flow conservation constraints.
        for (int i = 0; i < data.numberJob; i++) {
            for (int k = 0; k < data.resourceCapacity.size(); k++) {

                GRBLinExpr expr1 = new GRBLinExpr();
                GRBLinExpr expr2 = new GRBLinExpr();

                for (int j = 0; j < data.numberJob; j++) {
                    if (i == j) continue; // Skip self-
                    expr1.addTerm(1, continuousFlowVars[i][j][k]);
                }

                expr2.addConstant(resourceDemands[i][k]);

                model.addConstr(expr1, GRB.EQUAL, expr2, expr1 + " equals " + expr2 + " (C16)");
            }
        }

        // (17) i think there is a typo and it should be "for all j" instead of "for all i" like in C16
        for (int j = 0; j < data.numberJob; j++) {
            for (int k = 0; k < data.resourceCapacity.size(); k++) {

                GRBLinExpr expr1 = new GRBLinExpr();
                GRBLinExpr expr2 = new GRBLinExpr();

                for (int i = 0; i < data.numberJob; i++) {
                   if (i == j) continue; // Skip self-
                    expr1.addTerm(1, continuousFlowVars[i][j][k]);
                }

                expr2.addConstant(resourceDemands[j][k]);

                model.addConstr(expr1, GRB.EQUAL, expr2, expr1 + " equals " + expr2 + " (C17)");
            }
        }

        // (18)
        for (int k = 0; k < data.resourceCapacity.size(); k++) {
            GRBLinExpr expr1 = new GRBLinExpr();
            GRBLinExpr expr2 = new GRBLinExpr();

            expr1.addTerm(1, continuousFlowVars[data.numberJob - 1][0][k]);
            expr2.addConstant(data.resourceCapacity.get(k));

            model.addConstr(expr1, GRB.EQUAL, expr2, expr1 + " equals " + expr2 + " (C18)");
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
                    model.addConstr(expr1, GRB.EQUAL, expr2, expr1 + " equals " + expr2 + " (C19)");
                }
                if (teMatrix[i][j] == 0) {
                    model.addConstr(expr1, GRB.EQUAL, expr3, expr1 + " equals " + expr3 + " (C20)");
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

                    model.addConstr(expr1, GRB.GREATER_EQUAL, expr2, expr1 + "non negative (C21)");
                }
            }
        }



        // (22) set dummy starter starting time at 0
        GRBLinExpr expr01 = new GRBLinExpr();
        GRBLinExpr expr02 = new GRBLinExpr();

        expr01.addTerm(1, startingTimeVars[0]);
        expr02.addConstant(0);

        model.addConstr(expr01, GRB.EQUAL, expr02, expr01 + "starting time " + expr02 + " (C22)");

        // (23) Starting time of variable x is between its earliest and latest starting time
        for (int i = 0; i < data.numberJob; i++) {
            GRBLinExpr expr1 = new GRBLinExpr();
            GRBLinExpr expr2 = new GRBLinExpr();
            GRBLinExpr expr3 = new GRBLinExpr();

            expr1.addTerm(1, startingTimeVars[i]);
            expr2.addConstant(earliestStartTime[i]);
            expr3.addConstant(latestStartTime[i]);

            model.addConstr(expr2, GRB.LESS_EQUAL, expr1, expr2 + " earliest possible start <= " +
                    expr1 + "(23)");
            model.addConstr(expr1, GRB.LESS_EQUAL, expr3, expr1 + " latest possible start >= " +
                    expr3 + "(23)");
        }

        // Objective function: minimize the starting time of the last job
        // This is equivalent to minimizing the makespan of the schedule
        GRBLinExpr obj = new GRBLinExpr();
        obj.addTerm(1, startingTimeVars[data.numberJob - 1]);
        model.setObjective(obj, GRB.MINIMIZE);

        return model;
    }

    private static int[][] generateEarliestAndLatestStartTimes(List<List<Integer>> jobPredecessors,
                                                             List<Integer> jobDuration, int horizon) {
        int[][] startTimes = new int[2][jobDuration.size()];

        int n = jobDuration.size();  // Number of nodes
        List<List<DAGLongestPath.Edge>> graph = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            graph.add(new ArrayList<>());
        }

        for (int i = 0; i < jobPredecessors.size(); i++) {
            for (int predecessor : jobPredecessors.get(i)) {
                graph.get(predecessor - 1).add(new DAGLongestPath.Edge(i, jobDuration.get(predecessor - 1)));
            }
        }

        int source = 0;
        int[] earliestStartTimes = DAGLongestPath.findLongestPaths(graph, source);
        int[] latestStartTimes = new int[jobDuration.size()];

        for (int i = 0; i < jobDuration.size(); i++) {
            int duration = DAGLongestPath.findLongestPaths(graph, i)[jobDuration.size() - 1];
            latestStartTimes[i] = horizon - duration;
        }


        /*for (int i = 0; i < earliestStartTimes.length; i++) {
            if (earliestStartTimes[i] == Integer.MIN_VALUE) {
                System.out.println("Node " + i + ": unreachable");
            } else {
                System.out.println("Node " + i + ": " + earliestStartTimes[i]);
                System.out.println("Node " + i + ": " + latestStartTimes[i]);
            }
        }*/

        startTimes[0] = earliestStartTimes;
        startTimes[1] = latestStartTimes;
        return startTimes;
    }

    public static int[][] computeTEMatrix(int jobCount, List<List<Integer>> jobSuccessors, List<Integer> jobDuration) {
        int[][] teMatrix = new int[jobCount][jobCount];

        for (int i = 0; i < jobCount; i++) {
            Set<Integer> allSuccessors = new HashSet<>();
            collectAllSuccessors(i, allSuccessors, jobSuccessors);
            for (int successor : allSuccessors) {
                teMatrix[i][successor] = 1;
            }
        }

        return teMatrix;
    }

    private static void collectAllSuccessors(int job, Set<Integer> result, List<List<Integer>> jobSuccessors) {
        for (int successor : jobSuccessors.get(job)) {
            successor -= 1;
            if (result.add(successor)) {
                collectAllSuccessors(successor, result, jobSuccessors);
            }
        }
    }

    public static int[][] computeTEMatrix2(int jobCount, List<List<Integer>> jobSuccessors, List<Integer> jobDuration) {
        int[][] teMatrix = new int[jobCount][jobCount];

        for (int i = 0; i < jobCount; i++) {
            boolean[] visited = new boolean[jobCount];
            collectCumulativeDurations(i, 0, visited, teMatrix, jobSuccessors, jobDuration);
        }

        return teMatrix;
    }

    private static void collectCumulativeDurations(
            int currentJob,
            int accumulatedDuration,
            boolean[] visited,
            int[][] teMatrix,
            List<List<Integer>> jobSuccessors,
            List<Integer> jobDuration
    ) {
        for (int successor : jobSuccessors.get(currentJob)) {
            int successorIndex = successor - 1;
            int newCumulativeDuration = accumulatedDuration + jobDuration.get(currentJob);

            // Update the matrix if no value has been set yet or if a shorter path is found
            if (teMatrix[currentJob][successorIndex] == 0 || teMatrix[currentJob][successorIndex] > newCumulativeDuration) {
                teMatrix[currentJob][successorIndex] = newCumulativeDuration;
            }

            collectCumulativeDurations(successorIndex, newCumulativeDuration, visited, teMatrix, jobSuccessors, jobDuration);
        }
    }

    public class FlowBasedContinuousTimeModelSolution implements ModelSolutionInterface {
        GRBVar[] startingTimeVars;// = new GRBVar[data.numberJob];
        GRBVar[][] precedenceVars;// =new GRBVar[data.numberJob][data.numberJob];
        GRBVar[][][] continuousFlowVars;// =new GRBVar[data.numberJob][data.numberJob][data.resourceCapacity.size()];


        public GRBModel model;
        public int[][] earliestLatestStartTimes;

        public FlowBasedContinuousTimeModelSolution(GRBVar[] startingTimeVars, GRBVar[][] precedenceVars,
                        GRBVar[][][] continuousFlowVars, GRBModel model, int[][] earliestLatestStartTimes) {
            
            this.startingTimeVars = startingTimeVars;
            this.precedenceVars = precedenceVars;
            this.continuousFlowVars = continuousFlowVars;
            this.model = model;
            this.earliestLatestStartTimes = earliestLatestStartTimes;
        }
    }

}