package oldtimer;

import com.gurobi.gurobi.*;

import io.FileReader;
import io.JobDataInstance;
import io.Result;
import models.HeuristicSerialSGS;

import java.util.*;

public class FirstTryOnDIscreteTimeModel {

    public static Result.ScheduleIntegerResult gurobiRcpspJ30(String file) throws Exception {
        // Create FileReader instance and get the data
        FileReader fileReader = new FileReader();
        JobDataInstance data = fileReader.dataRead(file);

        // RcpspParser rcpspParser = new RcpspParser();
        // RcpspParser.dataInstance dataInstance = rcpspParser.readFile(file);

        // T=100 like its recommended for J30 instances (time to solve)
        final int T = 100;

        // Initialize the Gurobi environment and model
        GRBEnv env = new GRBEnv();
        GRBModel model = new GRBModel(env);

        GRBVar[][] x = new GRBVar[data.numberJob][T];
        fillWithVariables(model, x, data.numberJob, T);

        setObjective(model, x, data.numberJob, T);

        addDoOnceConstraints(model, x, data.numberJob, T);

        addTimeConstraints(model, x, data.jobPredecessors, data.jobDuration, data.numberJob, T);

        addResourceConstraints(model, x, data.resourceCapacity, data.jobResource, data.jobDuration, data.horizon,
                data.numberJob, T);

        // apply serial SGS start Solution to model
        applySolutionWithGurobi(model, data.numberJob, data.jobDuration, HeuristicSerialSGS.serialSGS(data));

        // Write model to file and optimize
        model.write("linear_model.lp");
        model.optimize();

        Result.ScheduleIntegerResult scheduleIntegerResult = fillListsToReturn(model, data.jobDuration, data.numberJob);

        // Clean up Gurobi model and environment
        model.dispose();
        env.dispose();

        return scheduleIntegerResult;
    }

    // Add variables xjt, activity j starts at time t
    private static void fillWithVariables(GRBModel model, GRBVar[][] x, int numJob, int T) throws GRBException {
        for (int i = 0; i < numJob; i++) {
            for (int t = 0; t < T; t++) {
                x[i][t] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "x[" + i + "," + t + "]");
            }
        }
    }

    private static void setObjective(GRBModel model,  GRBVar[][] x, int numJob, int T) throws GRBException {
        // Set the objective (makespan), last element is dummy element (only one without successor)
        GRBLinExpr obj = new GRBLinExpr();
        for (int t = 0; t < T; t++) {
            obj.addTerm(t, x[numJob - 1][t]);
        }
        model.setObjective(obj, GRB.MINIMIZE);
    }

    private static void addDoOnceConstraints(GRBModel model, GRBVar[][] x, int numberJob, int T) throws GRBException {
        // Constraint: each job can only be done once
        for (int i = 0; i < numberJob; i++) {
            GRBLinExpr expr = new GRBLinExpr();
            for (int t = 0; t < T; t++) {
                expr.addTerm(1.0, x[i][t]);
            }
            model.addConstr(expr, GRB.EQUAL, 1.0, "singleStart_" + i);
        }
    }

    private static void addTimeConstraints(GRBModel model, GRBVar[][] x, List<List<Integer>> jobPredecessors,
                                           List<Integer> jobDuration, int numberJob, int T) throws GRBException {
        // Timing constraints
        for (int i = 0; i < numberJob; i++) {
            // if job has predecessors
            if (!jobPredecessors.get(i).isEmpty()) {
                for (int j : jobPredecessors.get(i)) {
                    GRBLinExpr sumTi = new GRBLinExpr();
                    GRBLinExpr sumTj = new GRBLinExpr();

                    for (int t0 = 0; t0 < T; t0++) {
                        sumTi.addTerm(t0, x[i][t0]);
                    }

                    for (int t1 = 0; t1 < T; t1++) {
                        sumTj.addTerm(t1 + jobDuration.get(j-1), x[j-1][t1]);
                    }

                    model.addConstr(sumTi, GRB.GREATER_EQUAL, sumTj, "timing_" + i + "_" + j);
                }
            }
        }
    }

    private static void addResourceConstraints(GRBModel model, GRBVar[][] x, List<Integer> resourceCapacity,
                                               List<List<Integer>> jobResource, List<Integer> jobDuration,
                                               int horizon, int numberJob, int T) throws GRBException {
        // Resource constraints
        // for each resource
        for (int k = 0; k < resourceCapacity.size(); k++) {
            // horizon
            for (int t3 = 0; t3 < horizon; t3++) {
                GRBLinExpr useResource = new GRBLinExpr();
                // for each job
                for (int j = 0; j < numberJob; j++) {
                    for (int t4 = t3; t4 < t3 + jobDuration.get(j) && t4 < T; t4++) {
                        // Add the resource demand of job j for resource k at time t4
                        // x[j][t4] = 1 if job j is scheduled at time t4
                        useResource.addTerm(jobResource.get(j).get(k), x[j][t4]);
                    }
                }
                model.addConstr(useResource, GRB.LESS_EQUAL, resourceCapacity.get(k),
                        "resource_" + k + "_" + t3);
            }
        }
    }

    private static void applySolutionWithGurobi(GRBModel model, int numberJob, List<Integer> jobDuration,
                                               List<Integer> startTimes) throws GRBException {
        model.update();
        for (int i = 0; i < numberJob; i++) {
            int startTime = startTimes.get(i); // Start time for job i
            for (int t = startTime; t < startTime + jobDuration.get(i); t++) {
                // Get the variable corresponding to job i and time t
                GRBVar var = model.getVarByName("x[" + i + "," + t + "]");
                if (var != null) {
                    // Set the start value for the variable
                    var.set(GRB.DoubleAttr.Start, 1.0);
                }
            }
        }

        model.update();
    }

    private static Result.ScheduleIntegerResult fillListsToReturn(GRBModel model, List<Integer> jobDuration, int numJob) throws GRBException {

        // Safe solution in outputDict
        Map<Integer, Integer> outputDict = new HashMap<>();
        for (GRBVar var : model.getVars()) {
            if (var.get(GRB.DoubleAttr.X) != 0) {
                System.out.println(var.get(GRB.StringAttr.VarName) + " " + var.get(GRB.DoubleAttr.X));
                String[] varParts = var.get(GRB.StringAttr.VarName)
                        .replace("x[", "")
                        .replace("]", "")
                        .split(",");
                outputDict.put(Integer.parseInt(varParts[0]), Integer.parseInt(varParts[1]));
            }
        }

        List<Integer> start = new ArrayList<>(numJob);
        List<Integer> finish = new ArrayList<>(numJob);

        for (int i = 0; i < numJob; i++) {
            start.add(0);
            finish.add(0);
        }

        // Fill start times
        for (Map.Entry<Integer, Integer> entry : outputDict.entrySet()) {
            start.set(entry.getKey(), entry.getValue());
        }

        // Fill finish times
        for (int i = 0; i < numJob; i++) {
            finish.set(i, start.get(i) + jobDuration.get(i));
        }
        return new Result.ScheduleIntegerResult(start, finish);
    }
}