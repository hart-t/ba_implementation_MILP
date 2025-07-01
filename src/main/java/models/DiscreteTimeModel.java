package models;

import com.gurobi.gurobi.*;

import io.JobDataInstance;
import utility.DAGLongestPath;

/*
     * Solves the RCPSP problem using Gurobi with a discrete time model.
     *  https://www.sciencedirect.com/science/article/pii/S0305054809003360
     *  Basic discrete-time formulation (DT), 1969, Pritsker et al.
     *
     *  public final int numberJob;
     *  public final int horizon;
     *  public final List<Integer> jobNumSuccessors;
     *  public final List<List<Integer>> jobSuccessors;
     *  public final List<List<Integer>> jobPredecessors;
     *  public final List<Integer> jobDuration;
     *  public final List<List<Integer>> jobResource;
     *  public final List<Integer> resourceCapacity;
     * 
     * Variables: x_{it} = 1 if activity i starts at time t, 0 otherwise
     * Objective: minimize makespan (start time of dummy end activity)
     * Constraints:
     * (4) Precedence constraints
     * (5) Resource constraints  
     * (6) Each activity starts exactly once
     * (7) Dummy start activity at time 0
     * (8)/(9) Variable bounds and binary constraints
     * (8) is replaced by setting startingTimeVars[i][t] = null for t element H\{ESi,LSi}
     * (9) is redundant by setting the Variable type to GRB.BINARY?
     */

public class DiscreteTimeModel {

    public static GRBModel gurobiRcpspJ30(GRBModel model, JobDataInstance data) throws Exception {

        int[][] startTimes = DAGLongestPath.generateEarliestAndLatestStartTimes
                (data.jobPredecessors, data.jobDuration, data.horizon);
        int[] earliestStartTime = startTimes[0];
        int[] latestStartTime = startTimes[1];

        GRBVar[][] startingTimeVars = new GRBVar[data.numberJob][data.horizon];
        for (int i = 0; i < data.numberJob; i++) {
            for (int t = 0; t < data.horizon; t++) {
                if (t >= earliestStartTime[i] && t <= latestStartTime[i]) {
                    startingTimeVars[i][t] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "startingTime[" +
                            i + "] at [" + t + "]");
                } else {
                    startingTimeVars[i][t] = null; // replaces (8)? = 0?
                }
            }
        }


        // (3) Set the objective: minimize sum_{t=ESi}^{LSi} t * x[n+1, t]
        GRBLinExpr obj = new GRBLinExpr();
        for (int t = earliestStartTime[data.numberJob - 1]; t <= latestStartTime[data.numberJob - 1]; t++) {
            if (t >= 0 && t < data.horizon && startingTimeVars[data.numberJob - 1][t] != null) {
                obj.addTerm(t, startingTimeVars[data.numberJob - 1][t]);
            }
        }
        model.setObjective(obj, GRB.MINIMIZE);

        // (4) Precedence constraints: sum_{t=ESj}^{LSj} t*x_{jt} >= sum_{t=ESi}^{LSi} t*x_{it} + p_i for all (i,j) in E
        // translations of precedence constraints 
        for (int j = 0; j < data.numberJob; j++) {
            for (int predecessor : data.jobPredecessors.get(j)) {
                int i = predecessor - 1;
                
                GRBLinExpr expr1 = new GRBLinExpr();
                GRBLinExpr expr2 = new GRBLinExpr();

                // Left: sum_{t=ESj}^{LSj} t * x[j][t]
                for (int t = earliestStartTime[j]; t <= latestStartTime[j]; t++) {
                    if (t >= 0 && t < data.horizon && startingTimeVars[j][t] != null) {
                        expr1.addTerm(t, startingTimeVars[j][t]);
                    }
                }
                // Right: sum_{t=ESi}^{LSi} t * x[i][t] + p_i
                for (int t = earliestStartTime[i]; t <= latestStartTime[i]; t++) {
                    if (t >= 0 && t < data.horizon && startingTimeVars[i][t] != null) {
                        expr2.addTerm(t, startingTimeVars[i][t]);
                    }
                }
                expr2.addConstant(data.jobDuration.get(i));

                model.addConstr(expr1, GRB.GREATER_EQUAL, expr2, "precedence_" + i + "_" + j);
            }
        }

        // (5) Resource constraints: sum_{i=1}^n b_{ik} * sum_{tau=max(ESi,t-pi+1)}^{min(LSi,t)} x_{i,tau} <= B_k
        for (int k = 0; k < data.resourceCapacity.size(); k++) {
            for (int t = 0; t < data.horizon; t++) {
                GRBLinExpr resourceUsage = new GRBLinExpr();
                
                for (int i = 0; i < data.numberJob; i++) {
                    int resourceDemand = data.jobResource.get(i).get(k);
                    if (resourceDemand > 0) {
                        // Job i is active at time t if it started at time tau where t-p_i+1 <= tau <= t
                        int tauMin = Math.max(earliestStartTime[i], t - data.jobDuration.get(i) + 1);
                        int tauMax = Math.min(latestStartTime[i], t);
                        
                        // check if range is valid
                        if (tauMin <= tauMax) {
                            for (int tau = tauMin; tau <= tauMax; tau++) {
                                if (tau >= 0 && tau < data.horizon && startingTimeVars[i][tau] != null) {
                                    resourceUsage.addTerm(resourceDemand, startingTimeVars[i][tau]);
                                }
                            }
                        }
                    }
                }
                
                model.addConstr(resourceUsage, GRB.LESS_EQUAL, data.resourceCapacity.get(k), 
                               "resource_" + k + "_time_" + t);
            }
        }

        // (6) Each activity must start exactly once
        for (int i = 0; i < data.numberJob; i++) {
            GRBLinExpr activityStart = new GRBLinExpr();
            for (int t = earliestStartTime[i]; t <= latestStartTime[i]; t++) {
                if (t >= 0 && t < data.horizon && startingTimeVars[i][t] != null) {
                    activityStart.addTerm(1.0, startingTimeVars[i][t]);
                }
            }
            model.addConstr(activityStart, GRB.EQUAL, 1.0, "activity_start_once_" + i);
        }

        // (7) Dummy start activity constraint: x_{00} = 1
        if (data.numberJob > 0 && startingTimeVars[0][0] != null) {
            model.addConstr(startingTimeVars[0][0], GRB.EQUAL, 1.0, "dummy_start");
        }

        // (8) Variable bounds 
        /*for (int i = 0; i < data.numberJob; i++) {
            for (int t = 0; t < data.horizon; t++) {
                if (t < earliestStartTime[i] || t > latestStartTime[i]) {
                    model.addConstr(startingTimeVars[i][t], GRB.EQUAL, 0.0, "bound_" + i + "_" + t);
                }
            }
        }*/

        // (9) Binary variables constraint not necessary cause vars are set to GRB.BINARY when creating variables

        // apply serial SGS start Solution to model
        // applySolutionWithGurobi(model, data.numberJob, data.jobDuration, HeuristicSerialSGS.serialSGS(data));

        return model;
    }

    /* 
    private static void applySolutionWithGurobi(GRBModel model, int numberJob, List<Integer> jobDuration,
                                                List<Integer> startTimes) throws GRBException {
        model.update();
        for (int i = 0; i < numberJob; i++) {
            int startTime = startTimes.get(i); // Start time for job i
            // Get the variable corresponding to job i starting at startTime
            GRBVar var = model.getVarByName("startingTime[" + i + "] at [" + startTime + "]");
            if (var != null) {
                // Set the start value for the variable to 1 (job i starts at startTime)
                var.set(GRB.DoubleAttr.Start, 1.0);
            }
        }
        model.update();
    }*/
}