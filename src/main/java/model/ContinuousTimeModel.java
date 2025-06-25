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

    public static Result.ScheduleDoubleResult gurobiRcpspJ30(String file) throws Exception {
        // Create FileReader instance and get the data
        FileReader fileReader = new FileReader();
        FileReader.JobData data = fileReader.dataRead(file);

        for (int i = 0; i < data.jobPredecessors.size(); i++) {
            for (int predecessor : data.jobPredecessors.get(i)) {
            }
        }

        // T=100 like its recommended for J30 instances (time to solve)
        final int T = 100;

        // Initialize the Gurobi environment and model
        GRBEnv env = new GRBEnv();
        GRBModel model = new GRBModel(env);

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
                precedenceVars[i][j] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "x_" + i +
                        "_precedes_" + j);
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
                            "flow_" + i + "_" + j + "_" + k);
                }
            }
        }

        // activity 0 acts as a resource source while activity acts as a resource sink.
        int[][] resourceDemands = new int[data.numberJob][data.jobResource.size()];
        for (int i = 0; i < data.numberJob; i++) {
            for (int k = 0; k < data.resourceCapacity.size(); k++) {
                if (i == 0) {
                    resourceDemands[i][k] = -data.resourceCapacity.get(k);  // Source provides resources (negative)
                } else if (i == data.numberJob - 1) {
                    resourceDemands[i][k] = data.resourceCapacity.get(k);   // Sink consumes resources (positive)
                } else {
                    resourceDemands[i][k] = data.jobResource.get(i).get(k); // Regular activities
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

        // generate TE matrix, This means that if there is a path from activity i to j in the precedence graph
        // (even if not directly), then (i,j) element TE.
        int[][] teMatrix = computeTEMatrix(data.numberJob, data.jobSuccessors, data.jobDuration);

        /*for (int[] row : teMatrix) {
            System.out.println(Arrays.toString(row));
        }*/

        int[][] startTimes = DAGLongestPath.generateEarliestAndLatestStartTimes
                (data.jobPredecessors, data.jobDuration, data.horizon);
        int[] earliestStartTime = startTimes[0];
        int[] latestStartTime = startTimes[1];

        // Constraints (14) are so-called disjunctive constraints linking the start time of i and j with respect to
        // variable xij.
        // p duration
        for (int i = 0; i < data.numberJob; i++) {
            for (int j = 0; j < data.numberJob; j++) {
                if (i == j) continue;
                if (precedenceVars[i][j] == null) continue;
                // Mij is some large enough constant, which can be set to any valid upper bound for Si - Sj
                // (e.g., Mij = ESi - LSj)
                // TODO make it make sense
                /*
                if i calculate Mij like its described as ESi - LSj and i precedes j then the Mij is negative?
                but the same discription said to set Mij as a large enough constant
                 */
                // Big-M should be large enough to make constraint non-binding when x_ij = 0
                int Mij = data.horizon + data.jobDuration.get(i);
                
                GRBLinExpr expr1 = new GRBLinExpr();
                expr1.addTerm(1, startingTimeVars[j]);
                expr1.addTerm(-1, startingTimeVars[i]);
                expr1.addConstant(-data.jobDuration.get(i));

                GRBLinExpr expr2 = new GRBLinExpr();
                expr2.addTerm(-Mij, precedenceVars[i][j]);
                expr2.addConstant(Mij);

                model.addConstr(expr1, GRB.GREATER_EQUAL, expr2, "disjunctive_" + i + "_" + j + " (C14)");
            }
        }

        // Constraints (15) link flow variables and xij variables. If i precedes j, the maximum flow sent
        // from i to j is set to min{bik, bjk} while if i does not precede j the flow must be zero.
        for (int i = 0; i < data.numberJob; i++) {
            for (int j = 0; j < data.numberJob; j++) {
                if (i == j) continue;
                if (precedenceVars[i][j] == null) continue;
                for (int k = 0; k < data.resourceCapacity.size(); k++) {
                    GRBLinExpr expr1 = new GRBLinExpr();
                    GRBLinExpr expr2 = new GRBLinExpr();

                    expr1.addTerm(1, continuousFlowVars[i][j][k]);
                    // Use absolute values for resource demands
                    int minResourceDemand = Math.min(Math.abs(resourceDemands[i][k]), Math.abs(resourceDemands[j][k]));
                    expr2.addTerm(minResourceDemand, precedenceVars[i][j]);

                    model.addConstr(expr1, GRB.LESS_EQUAL, expr2, "flow_bound_" + i + "_" + j + "_" + k + " (C15)");
                }
            }
        }

        // Constraints (16), (17), (18) are resource flow conservation constraints.
        // (16) Outflow from activity i equals resource demand (for source: total capacity, others: actual demand)
        for (int i = 0; i < data.numberJob; i++) {
            for (int k = 0; k < data.resourceCapacity.size(); k++) {
                GRBLinExpr outflow = new GRBLinExpr();

                for (int j = 0; j < data.numberJob; j++) {
                    if (i == j) continue; // Skip self-
                    outflow.addTerm(1, continuousFlowVars[i][j][k]);
                }

                // Use the actual resource demand values (not absolute)
                model.addConstr(outflow, GRB.EQUAL, resourceDemands[i][k], "outflow_" + i + "_" + k + " (C16)");
            }
        }

        // (17) Inflow to activity j equals resource demand (for sink: total capacity, others: actual demand)
        for (int j = 0; j < data.numberJob; j++) {
            for (int k = 0; k < data.resourceCapacity.size(); k++) {
                GRBLinExpr inflow = new GRBLinExpr();

                for (int i = 0; i < data.numberJob; i++) {
                    if (i == j) continue; // Skip self-
                    inflow.addTerm(1, continuousFlowVars[i][j][k]);
                }

                // Use the actual resource demand values (not absolute)
                model.addConstr(inflow, GRB.EQUAL, resourceDemands[j][k], "inflow_" + j + "_" + k + " (C17)");
            }
        }

        // (18) - Flow from sink back to source to close the circuit
        for (int k = 0; k < data.resourceCapacity.size(); k++) {
            GRBLinExpr expr1 = new GRBLinExpr();
            GRBLinExpr expr2 = new GRBLinExpr();

            expr1.addTerm(1, continuousFlowVars[data.numberJob - 1][0][k]);
            expr2.addConstant(data.resourceCapacity.get(k));

            model.addConstr(expr1, GRB.EQUAL, expr2, "flow_sink_to_source_resource_" + k + " (C18)");
        }



        // Constraints (19), (20) set the preexisting precedence constraints.
        for (int i = 0; i < data.numberJob; i++) {
            for (int j = 0; j < data.numberJob; j++) {
                if (i == j) continue;

                if (teMatrix[i][j] == 1) {
                    // If there's a precedence relation, force x_ij = 1
                    model.addConstr(precedenceVars[i][j], GRB.EQUAL, 1, "precedence_" + i + "_" + j + " (C19)");
                }
                if (teMatrix[i][j] == 0) {
                    // If there's no precedence relation, force x_ij = 0
                    model.addConstr(precedenceVars[i][j], GRB.EQUAL, 0, "no_precedence_" + i + "_" + j + " (C20)");
                }
            }
        }

        // (21) non-negative constraint for flow variables - REMOVED as redundant
        // Non-negativity is already enforced by variable bounds in addVar() calls



        // (22) set dummy starter starting time at 0
        GRBLinExpr expr01 = new GRBLinExpr();
        GRBLinExpr expr02 = new GRBLinExpr();

        expr01.addTerm(1, startingTimeVars[0]);
        expr02.addConstant(0);

        model.addConstr(expr01, GRB.EQUAL, expr02, "dummy_start_at_zero (C22)");

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

        // Update model to ensure all variables and constraints are integrated
        model.update();

        //GRBVar[][] x = new GRBVar[data.numberJob][T];
        //fillWithCVariables(model, startingTimeVars, precedenceVars, continuousFlowVars, resourceDemands);

        // Write model to file and optimize
        // T=100 like its recommended for J30 instances (time to solve)
        model.set(GRB.DoubleParam.TimeLimit, 100);
        model.write("linear_model.lp");
        model.optimize();

        if (model.get(GRB.IntAttr.Status) == GRB.Status.INFEASIBLE) {
            System.out.println("Model is infeasible.");
            model.computeIIS();
            model.write("model.ilp");
            
            // Return empty result for infeasible model
            List<Double> emptyStart = new ArrayList<>(Collections.nCopies(data.numberJob, 0.0));
            List<Double> emptyFinish = new ArrayList<>(Collections.nCopies(data.numberJob, 0.0));
            return new Result.ScheduleDoubleResult(emptyStart, emptyFinish);
        }

        Result.ScheduleDoubleResult scheduleDoubleResult = fillListsToReturn(model, data.jobDuration, data.numberJob);

        // Clean up Gurobi model and environment
        model.dispose();
        env.dispose();

        return scheduleDoubleResult;
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

    private static Result.ScheduleDoubleResult fillListsToReturn(GRBModel model, List<Integer> jobDuration, int numJob) throws GRBException {

        // Safe solution in outputDict
        Map<Integer, Double> outputDict = new HashMap<>();
        for (int i = 0; i < numJob; i++) {
            GRBVar var = model.getVars()[i];
            System.out.println(var.get(GRB.StringAttr.VarName) + " " + var.get(GRB.DoubleAttr.X));
            outputDict.put(i, var.get(GRB.DoubleAttr.X));
        }

        List<Double> start = new ArrayList<>(numJob);
        List<Double> finish = new ArrayList<>(numJob);

        for (int i = 0; i < numJob; i++) {
            start.add(0.0);
            finish.add(0.0);
        }

        // Fill start times
        for (Map.Entry<Integer, Double> entry : outputDict.entrySet()) {
            start.set(entry.getKey(), entry.getValue());
        }

        // Fill finish times
        for (int i = 0; i < numJob; i++) {
            finish.set(i, start.get(i) + jobDuration.get(i));
        }
        return new Result.ScheduleDoubleResult(start, finish);
    }
}
