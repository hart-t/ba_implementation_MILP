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
        GRBVar[] startingTimeVars =new GRBVar[data.numberJob];
        for (int i = 0; i < data.numberJob; i++) {
            startingTimeVars[i] = model.addVar(0.0, data.horizon, 0.0, GRB.CONTINUOUS, "startingTime[" +
                    i + "]");
        }

        //indicate whether activity i is processed before activity j
        GRBVar[][] precedenceVars =new GRBVar[data.numberJob][data.numberJob];
        for (int i = 0; i < data.numberJob; i++) {
            for (int j = 0; j < data.numberJob; j++) {
                precedenceVars[i][j] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "[" + i +
                        "] precedes [" + j + "]");
            }
        }

        //Finally, continuous flow variables are introduced to denote the quantity of resource k that is transferred
        // from activity i (at the end of its processing) to activity j (at the start of its processing).
        GRBVar[][][] continuousFlowVars =new GRBVar[data.numberJob][data.numberJob][data.resourceCapacity.size()];
        for (int i = 0; i < data.numberJob; i++) {
            for (int j = 0; j < data.numberJob; j++) {
                for (int k = 0; k < data.resourceCapacity.size(); k++) {
                    continuousFlowVars[i][j][k] = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.INTEGER,
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

        //generate TE matrix, This means that if there is a path from activity i to j in the precedence graph
        //(even if not directly), then (i,j) element TE.
        int[][] teMatrix = computeTEMatrix(data.numberJob, data.jobSuccessors, data.jobDuration);

        for (int[] row : teMatrix) {
            System.out.println(Arrays.toString(row));
        }

        int[][] precedenceActivityOnNodeGraph = generatePrecedenceActivityOnNodeGraph(data.jobPredecessors,
                data.jobDuration);

        System.out.println("-------------------------------------------------------------------------------------");

        for (int[] row : precedenceActivityOnNodeGraph) {
            System.out.println(Arrays.toString(row));
        }

        int[][] startTimes = generateEarliestAndLatestStartTimes(data.jobPredecessors, data.jobDuration, data.horizon);
        int[] earliestStartTime = startTimes[0];
        int[] latestStartTime = startTimes[1];

        //earliestStartList = generateEarliestStartList(teMatrix, data.jobDuration);
        //latestStartList = generateLatestStartList(teMatrix, data.jobDuration);




        //TODO
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



        //Constraints (19), (20) set the preexisting precedence constraints.
        for (int i = 0; i < data.numberJob; i++) {
            for (int j = 0; j < data.numberJob; j++) {
                GRBLinExpr expr1 = new GRBLinExpr();
                GRBLinExpr expr2 = new GRBLinExpr();
                GRBLinExpr expr3 = new GRBLinExpr();

                expr1.addTerm(teMatrix[i][j], precedenceVars[i][j]);
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
        GRBLinExpr expr01 = new GRBLinExpr();
        GRBLinExpr expr02 = new GRBLinExpr();

        expr01.addTerm(1, startingTimeVars[0]);
        expr02.addConstant(0);

        model.addConstr(expr01, GRB.EQUAL, expr02, expr01.toString() + "starting time " + expr02.toString()
                        + "(C22)");

        //(23) Starting time of variable x is between its earliest and latest starting time
        for (int i = 0; i < data.numberJob; i++) {
            GRBLinExpr expr1 = new GRBLinExpr();
            GRBLinExpr expr2 = new GRBLinExpr();
            GRBLinExpr expr3 = new GRBLinExpr();

            expr1.addTerm(1, startingTimeVars[i]);
            expr2.addConstant(earliestStartTime[i]);
            expr3.addConstant(latestStartTime[i]);

            model.addConstr(expr2, GRB.LESS_EQUAL, expr1, expr2.toString() + " earliest possible start <= " +
                    expr1.toString());
            model.addConstr(expr1, GRB.LESS_EQUAL, expr3, expr1.toString() + " latest possible start >= " +
                    expr3.toString());
        }

        //TODO braucht man das wenn GRB.BINARY?
        //(24) xij is binary
        for (int i = 0; i < data.numberJob; i++) {
            for (int j = 0; j < data.numberJob; j++) {
                GRBLinExpr expr1 = new GRBLinExpr();
                GRBLinExpr expr2 = new GRBLinExpr();

                expr1.addTerm(1, precedenceVars[i][j]);
                expr2.addConstant(1);
            }
        }





        //GRBVar[][] x = new GRBVar[data.numberJob][T];
        //fillWithCVariables(model, startingTimeVars, precedenceVars, continuousFlowVars, resourceDemands);

        // Write model to file and optimize
        model.write("linear_model.lp");
        model.optimize();


        // Clean up Gurobi model and environment
        model.dispose();
        env.dispose();
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


        for (int i = 0; i < earliestStartTimes.length; i++) {
            if (earliestStartTimes[i] == Integer.MIN_VALUE) {
                System.out.println("Node " + i + ": unreachable");
            } else {
                System.out.println("Node " + i + ": " + earliestStartTimes[i]);
                System.out.println("Node " + i + ": " + latestStartTimes[i]);
            }
        }

        startTimes[0] = earliestStartTimes;
        startTimes[1] = latestStartTimes;
        return startTimes;
    }

    private static int[][] generatePrecedenceActivityOnNodeGraph(List<List<Integer>> jobPredecessors,
                                                                 List<Integer> jobDuration) {
        int[][] precedenceActivityOnNodeGraph = new int[jobDuration.size()][jobDuration.size()];

        for (int i = 0; i < jobDuration.size(); i++) {
            for (int j = 0; j < jobDuration.size(); j++) {
                precedenceActivityOnNodeGraph[i][j] = -1;
            }
        }

        for (int i = 0; i < jobPredecessors.size(); i++) {
            for (int predecessor : jobPredecessors.get(i)) {
                precedenceActivityOnNodeGraph[predecessor - 1][i] = jobDuration.get(predecessor - 1);
            }
        }

        return precedenceActivityOnNodeGraph;
    }

    private static void fillWithCVariables(GRBModel model, GRBVar[] startingTimeVars, GRBVar[][] precedenceVars,
                                           GRBVar[][][] continuousFlowVars, int[][] resourceDemands) {



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

}
