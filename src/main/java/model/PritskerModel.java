package model;

import com.gurobi.gurobi.*;

import java.util.*;

public class PritskerModel {
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

        // Create decision variables
        // x[i][t] = 1 if job i starts at time t, 0 otherwise
        GRBVar[][] x = new GRBVar[numJobs][horizon];
        for (int i = 0; i < numJobs; i++) {
            for (int t = 0; t < horizon; t++) {
                x[i][t] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "x_" + i + "_" + t);
            }
        }

        // Each job must start exactly once
        for (int i = 0; i < numJobs; i++) {
            GRBLinExpr startOnceExpr = new GRBLinExpr();
            for (int t = 0; t < horizon; t++) {
                startOnceExpr.addTerm(1.0, x[i][t]);
            }
            model.addConstr(startOnceExpr, GRB.EQUAL, 1.0, "start_once_" + i);
        }

        // Precedence constraints
        for (int i = 0; i < numJobs; i++) {
            for (int j : successors[i]) {
                GRBLinExpr precedenceExpr = new GRBLinExpr();
                for (int t = 0; t < horizon; t++) {
                    precedenceExpr.addTerm(t, x[i][t]); // Start time of job i
                    precedenceExpr.addTerm(-t, x[j][t]); // Start time of job j
                }
                model.addConstr(precedenceExpr, GRB.LESS_EQUAL, -durations[i], "precedence_" + i + "_" + j);
            }
        }

        // Resource constraints
        for (int r = 0; r < numResources; r++) {
            for (int t = 0; t < horizon; t++) {
                GRBLinExpr resourceExpr = new GRBLinExpr();
                for (int i = 0; i < numJobs; i++) {
                    // Sum up resource usage for all jobs that could be active at time t
                    for (int tau = Math.max(0, t - durations[i] + 1); tau <= t; tau++) {
                        if (tau < horizon) {
                            resourceExpr.addTerm(demands[i][r], x[i][tau]);
                        }
                    }
                }
                model.addConstr(resourceExpr, GRB.LESS_EQUAL, resourceCaps[r], 
                              "resource_" + r + "_time_" + t);
            }
        }

        // Objective: Minimize makespan
        GRBLinExpr makespan = new GRBLinExpr();
        for (int i = 0; i < numJobs; i++) {
            for (int t = 0; t < horizon; t++) {
                makespan.addTerm(t + durations[i], x[i][t]);
            }
        }
        model.setObjective(makespan, GRB.MINIMIZE);

        return model;
    }
}