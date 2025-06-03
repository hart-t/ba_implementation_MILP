package model;

import com.gurobi.gurobi.*;
import java.util.*;

public class Test {
    public static class ScheduleResult {
        public final List<Integer> start;
        public final List<Integer> finish;

        public ScheduleResult(List<Integer> start, List<Integer> finish) {
            this.start = start;
            this.finish = finish;
        }
    }

    public static ScheduleResult gurobiRcpspJ30(String file) throws Exception {
        // Create FileReader instance and get the data
        FileReader fileReader = new FileReader();
        FileReader.JobData data = fileReader.dataRead(file);

        // Set the upper bound Completion Time of the project
        // we set T=100 when solving the J30 problem
        final int T = 100;

        // Initialize the Gurobi environment and model
        GRBEnv env = new GRBEnv();
        GRBModel model = new GRBModel(env);

        // Add variables xjt note that j activity starts at time t
        GRBVar[][] x = new GRBVar[data.numberJob][T];
        for (int i = 0; i < data.numberJob; i++) {
            for (int t = 0; t < T; t++) {
                x[i][t] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "x[" + i + "," + t + "]");
            }
        }

        // Set the objective which means minimize the Completion Time
        GRBLinExpr obj = new GRBLinExpr();
        for (int t = 0; t < T; t++) {
            obj.addTerm(t, x[data.numberJob - 1][t]);
        }
        model.setObjective(obj, GRB.MINIMIZE);

        // Constraint: each job can only be done once
        for (int i = 0; i < data.numberJob; i++) {
            GRBLinExpr expr = new GRBLinExpr();
            for (int t = 0; t < T; t++) {
                expr.addTerm(1.0, x[i][t]);
            }
            model.addConstr(expr, GRB.EQUAL, 1.0, "oneJob_" + i);
        }

        // Timing constraints
        for (int i = 0; i < data.numberJob; i++) {
            if (!data.jobPredecessors.get(i).isEmpty()) {
                for (int j : data.jobPredecessors.get(i)) {
                    GRBLinExpr sumTi = new GRBLinExpr();
                    GRBLinExpr sumTj = new GRBLinExpr();

                    for (int t0 = 0; t0 < T; t0++) {
                        sumTi.addTerm(t0, x[i][t0]);
                    }

                    for (int t1 = 0; t1 < T; t1++) {
                        sumTj.addTerm(t1 + data.jobDuration.get(j-1), x[j-1][t1]);
                    }

                    model.addConstr(sumTi, GRB.GREATER_EQUAL, sumTj, "timing_" + i + "_" + j);
                }
            }
        }

        // Resource constraints
        final int NUMBER_RESOURCE = 4;
        for (int k = 0; k < NUMBER_RESOURCE; k++) {
            for (int t3 = 0; t3 < 50; t3++) {
                GRBLinExpr useResource = new GRBLinExpr();
                for (int j = 0; j < data.numberJob; j++) {
                    for (int tt = t3; tt < t3 + data.jobDuration.get(j) && tt < T; tt++) {
                        useResource.addTerm(data.jobResource.get(j).get(k), x[j][tt]);
                    }
                }
                model.addConstr(useResource, GRB.LESS_EQUAL, data.resourceCapacity.get(k),
                        "resource_" + k + "_" + t3);
            }
        }

        // Write model to file and optimize
        model.write("linear_model.lp");
        model.optimize();

        // Get the solution
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

        List<Integer> start = new ArrayList<>(data.numberJob);
        List<Integer> finish = new ArrayList<>(data.numberJob);

        // Initialize lists with correct size
        for (int i = 0; i < data.numberJob; i++) {
            start.add(0);
            finish.add(0);
        }

        // Fill start times
        for (Map.Entry<Integer, Integer> entry : outputDict.entrySet()) {
            start.set(entry.getKey(), entry.getValue());
        }

        // Calculate finish times
        for (int i = 0; i < data.numberJob; i++) {
            finish.set(i, start.get(i) + data.jobDuration.get(i));
        }

        System.out.println("start: " + start);
        System.out.println("finish: " + finish);

        // Clean up Gurobi model and environment
        model.dispose();
        env.dispose();

        return new ScheduleResult(start, finish);
    }
}