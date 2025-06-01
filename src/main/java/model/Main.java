package model;

import com.gurobi.gurobi.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

import static model.RCPSP_Parser.parseFile;

public class Main {
    public static void main(String[] args) {
        System.out.println("Hello and welcome!");

        try {
            File[] files = new File("/home/tobsi/university/kit/benchmarkSets").listFiles();
            assert files != null;

            System.out.println("/home/tobsi/university/kit/benchmarkSets/" + files[0].getName());
            RCPSP_Parser.RCPSPInstance instance = parseFile("/home/tobsi/university/kit/benchmarkSets/" + files[0].getName());

            GRBEnv env = new GRBEnv(true);
            env.set("logFile", "rcpsp.log");
            env.start();

            GRBModel model = new GRBModel(env);

            int numJobs = instance.numberOfJobs;
            int horizon = instance.horizon;
            int[] durations = instance.jobDurations;
            int[][] successors = instance.jobSuccessors;
            int[][] demands = instance.resourceRequirements;
            int[] resourceCaps = instance.resourceAvailabilities;
            int numResources = instance.numberOfResources;

            List<Integer> T = new ArrayList<>();
            for (int t = 0; t <= horizon; t++) {
                T.add(t);
            }

            // Create binary decision variables x[i][t]
            Map<String, GRBVar> x = new HashMap<>();
            for (int i = 0; i < numJobs; i++) {
                for (int t : T) {
                    String varName = "x_" + i + "_" + t;
                    x.put(varName, model.addVar(0.0, 1.0, 0.0, GRB.BINARY, varName));
                }
            }

            // Objective: minimize finish time of last job (n-1)
           GRBLinExpr totalCompletionTime = new GRBLinExpr();
           for (int i = 0; i < numJobs; i++) {
               for (int t : T) {
                   String key = "x_" + i + "_" + t;
                   if (x.containsKey(key)) {
                       totalCompletionTime.addTerm(t, x.get(key));
                   }
               }
           }
           model.setObjective(totalCompletionTime, GRB.MINIMIZE);

            // Each job must start exactly once
            for (int i = 0; i < numJobs; i++) {
                GRBLinExpr startOnce = new GRBLinExpr();
                for (int t : T) {
                    String key = "x_" + i + "_" + t;
                    if (x.containsKey(key)) {
                        startOnce.addTerm(1.0, x.get(key));
                    }
                }
                model.addConstr(startOnce, GRB.EQUAL, 1.0, "StartOnce_" + i);
            }

            // Precedence constraints
            for (int i = 0; i < numJobs; i++) {
                for (int successor : successors[i]) {
                    if (successor >= 0 && successor < numJobs) {  // Validate successor index
                        GRBLinExpr startJ = new GRBLinExpr();
                        GRBLinExpr startI = new GRBLinExpr();
                        for (int t : T) {
                            String keyJ = "x_" + successor + "_" + t;
                            String keyI = "x_" + i + "_" + t;
                            if (x.containsKey(keyJ)) {
                                startJ.addTerm(t, x.get(keyJ));
                            }
                            if (x.containsKey(keyI)) {
                                startI.addTerm(t, x.get(keyI));
                            }
                        }
                        model.addConstr(startJ, GRB.GREATER_EQUAL, startI, "Pre_" + i + "_" + successor);
                    }
                }
            }

            // Resource constraints
            for (int r = 0; r < numResources; r++) {
                for (int t = 0; t <= horizon; t++) {
                    GRBLinExpr usage = new GRBLinExpr();
                    for (int j = 0; j < numJobs; j++) {
                        assert demands[j].length == numResources : "Mismatch in resource constraints.";
                        for (int t2 = t; t2 < Math.min(t + durations[j], horizon + 1); t2++) { // Ensure range is correct
                            String key = "x_" + j + "_" + t2;
                            if (x.containsKey(key)) {
                                usage.addTerm(demands[j][r], x.get(key));
                            }
                        }
                    }
                    model.addConstr(usage, GRB.LESS_EQUAL, resourceCaps[r], "Res_" + r + "_" + t);
                }
            }

            // After optimizing the model
            model.optimize();

            // Output results
            double makespan = 0.0;
            for (int i = 0; i < numJobs; i++) {
                for (int t : T) {
                    String key = "x_" + i + "_" + t;
                    if (x.containsKey(key) && x.get(key).get(GRB.DoubleAttr.X) > 0.5) {
                        System.out.println("Job " + i + " starts at time " + t);
                        
                        // Check if this is the last job and update the makespan
                        if (i == numJobs - 1 && t > makespan) {
                            makespan = t;
                        }
                    }
                }
            }

            // Output the makespan
            System.out.println("Makespan (total project duration): " + makespan);

            model.optimize();

            if (model.get(GRB.IntAttr.Status) == GRB.INFEASIBLE) {
                System.out.println("Model is infeasible. Check constraints or input data.");
                model.computeIIS(); // Compute irreducible infeasible subsystem
                model.write("infeasible.ilp"); // Write IIS to file
            }

            // Print results
            for (int i = 0; i < numJobs; i++) {
                for (int t : T) {
                    String key = "x_" + i + "_" + t;
                    if (x.containsKey(key) && x.get(key).get(GRB.DoubleAttr.X) > 0.5) {
                        System.out.println("Job " + i + " starts at time " + t);
                    }
                }
            }

            model.dispose();
            env.dispose();

        } catch (GRBException e) {
            System.out.println("Error code: " + e.getErrorCode() + ". " + e.getMessage());
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }
}