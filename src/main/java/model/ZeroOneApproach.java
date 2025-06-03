package model;

import com.gurobi.gurobi.*;

import java.util.*;

public class ZeroOneApproach {

    GRBModel model;

    public GRBModel fillModel(RCPSP_Parser.RCPSPInstance instance, GRBModel model) throws GRBException {
        int numJobs = instance.numberOfJobs; // number of jobs in the instance
        int horizon = instance.horizon; // maximum horizon for the instance
        int[] durations = instance.jobDurations; // durations[i] contains the duration of job i
        int[][] successors = instance.jobSuccessors; // successors[i] contains the successors of job i
        int[][] demands = instance.resourceRequirements; // demands[i][r] is the demand of job i for resource r
        int[] resourceCaps = instance.resourceAvailabilities; // resourceCaps[r] is the maximum available capacity of resource r
        int numResources = instance.numberOfResources; // total number of resources
        this.model = model;


        List<Integer> T = new ArrayList<>();
        for (int t = 0; t <= horizon; t++) {
            T.add(t);
        }

        // Create binary decision variables x[i][t]
        Map<String, GRBVar> x = new HashMap<>();
        for (int i = 0; i < numJobs; i++) {
            for (int t : T) { // Ensure all times from 0 to horizon are included
                String varName = "x_" + i + "_" + t;
                x.put(varName, model.addVar(0.0, 1.0, 0.0, GRB.BINARY, varName));
            }
        }

        /*
        // DEBUGGGGGG
        // Validate all successors to ensure required variables exist
        for (int i = 0; i < numJobs; i++) {
            for (int successor : successors[i]) {
                String successorVar = "x_" + successor + "_" + horizon;
                String currentVar = "x_" + (i + 1) + "_" + horizon;

                if (!x.containsKey(currentVar)) {
                    System.err.println("Error: Missing variable for job at horizon: " + currentVar);
                }

                if (!x.containsKey(successorVar)) {
                    System.err.println("Error: Missing variable for successor at horizon: " + successorVar);
                }
            }
        }
        */



        // Objective: minimize finish time of last job (n-1)
        GRBLinExpr totalCompletionTime = new GRBLinExpr();
        for (int i = 0; i < numJobs; i++) {
            for (int t : T) {
                String key = "x_" + i + "_" + t;
                if (x.containsKey(key)) {
                    totalCompletionTime.addTerm(t, x.get(key));
                    //System.out.println(x.get(key).get(GRB.StringAttr.VarName));
                    //System.out.println(key);
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
                    //System.out.println(x.get(key).get(GRB.StringAttr.VarName));
                    //System.out.println(key);
                }
            }
            model.addConstr(startOnce, GRB.EQUAL, 1.0, "StartOnce_" + i);
        }


/*
        // Ensure all variables exist before adding constraints
        for (int i = 0; i < numJobs; i++) {
            int[] successorsList = successors[i];
            for (int successor : successorsList) {
                // Construct the variable keys
                String var_i_horizon = "x_" + i + "_" + horizon;
                String var_successor_horizon = "x_" + successor + "_" + horizon;

                // Check if the variables exist in x before using them
                if (x.containsKey(var_i_horizon) && x.containsKey(var_successor_horizon)) {
                    // Add the precedence constraint
                    model.addConstr(x.get(var_i_horizon), GRB.GREATER_EQUAL, x.get(var_successor_horizon), "Pre_" + (i + 1) + "_" + successor);
                } else {
                    System.err.println("Warning: Variable not found for constraint: " + var_i_horizon + " or " + var_successor_horizon);
                }
            }
        }
*/

        // Resource constraints
        for (int r = 0; r < numResources; r++) {
            for (int t = 0; t <= horizon; t++) {
                GRBLinExpr usage = new GRBLinExpr();
                for (int j = 0; j < numJobs; j++) {
                    for (int t2 = Math.max(0, t - durations[j] + 1); t2 <= t; t2++) { // Ensure range is correct
                        String key = "x_" + j + "_" + t2;
                        if (x.containsKey(key)) {
                            usage.addTerm(demands[j][r], x.get(key));
                        }
                    }
                }
                model.addConstr(usage, GRB.LESS_EQUAL, resourceCaps[r], "Res_" + r + "_" + t);
            }
        }

        return model;
    }
}
