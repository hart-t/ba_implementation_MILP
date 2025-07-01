package oldtimer;

import com.gurobi.gurobi.*;

import model.DAGLongestPath;
import model.FileReader;
import model.Result;
import model.FileReader.JobData;
import model.Result.ScheduleDoubleResult;

import java.util.*;

/**
 * The On-Off Event-Based formulation for RCPSP from Koné et al. 2011
 * https://www.sciencedirect.com/science/article/pii/S0305054809003360
 * 
 * Variables: 
 * - S_i: continuous start time of activity i
 * - y_{it}: binary variable = 1 if activity i is active at time t, 0 otherwise
 * 
 */
public class OnOffEventBasedModel {

    public static GRBModel gurobiRcpspJ30(GRBModel model, FileReader.JobData data) throws Exception {
        // Calculate earliest and latest start times for variable bounds
        int[][] startTimes = DAGLongestPath.generateEarliestAndLatestStartTimes
                (data.jobPredecessors, data.jobDuration, data.horizon);
        int[] earliestStartTime = startTimes[0];
        int[] latestStartTime = startTimes[1];

        // Decision Variables
        
        // S_i: continuous start time variables
        GRBVar[] startingTimeVars = new GRBVar[data.numberJob];
        for (int i = 0; i < data.numberJob; i++) {
            startingTimeVars[i] = model.addVar(earliestStartTime[i], latestStartTime[i], 0.0, 
                    GRB.CONTINUOUS, "startingTime[" + i + "]");  // ← Changed from "S_" + i
        }

        // y_{it}: binary on-off variables (activity i active at time t)
        // Note: dummy activities with duration 0 don't need on-off variables
        GRBVar[][] onOffVars = new GRBVar[data.numberJob][data.horizon];
        for (int i = 0; i < data.numberJob; i++) {
            int duration = data.jobDuration.get(i);
            
            // Skip creating on-off variables for dummy activities (duration 0)?
            if (duration == 0) {
                for (int t = 0; t < data.horizon; t++) {
                    onOffVars[i][t] = null;
                }
                continue;
            }
            
            for (int t = 0; t < data.horizon; t++) {
                // Only create variables for valid time windows where activity could be active
                // Activity i can be active at time t if: earliestStartTime[i] <= t <= latestStartTime[i] + duration - 1
                if (t >= earliestStartTime[i] && t <= latestStartTime[i] + duration - 1) {
                    onOffVars[i][t] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "y_" + i + "_" + t);
                } else {
                    onOffVars[i][t] = null;
                }
            }
        }

        int onOffVarCount = 0;
        for (int i = 0; i < data.numberJob; i++) {
            for (int t = 0; t < data.horizon; t++) {
                if (onOffVars[i][t] != null) {
                    onOffVarCount++;
                }
            }
        }
        
        System.out.println("Created variables: " + data.numberJob + " start time vars, " + 
                          onOffVarCount + " on-off vars (excluding dummy activities)");

        // Objective function (41): minimize makespan (start time of dummy end activity)
        GRBLinExpr obj = new GRBLinExpr();
        obj.addTerm(1, startingTimeVars[data.numberJob - 1]);
        model.setObjective(obj, GRB.MINIMIZE);

        // Constraints

        // Precedence constraints: S_j >= S_i + p_i for all (i,j) in E
        System.out.println("Adding precedence constraints...");
        int precedenceCount = 0;
        for (int j = 0; j < data.numberJob; j++) {
            for (int pred : data.jobPredecessors.get(j)) {
                int i = pred - 1;
                if (i >= 0 && i < data.numberJob) {
                    GRBLinExpr lhs = new GRBLinExpr();
                    lhs.addTerm(1, startingTimeVars[j]);
                    
                    GRBLinExpr rhs = new GRBLinExpr();
                    rhs.addTerm(1, startingTimeVars[i]);
                    rhs.addConstant(data.jobDuration.get(i));
                    
                    model.addConstr(lhs, GRB.GREATER_EQUAL, rhs, "precedence_" + i + "_" + j);
                    precedenceCount++;
                }
            }
        }
        System.out.println("Added " + precedenceCount + " precedence constraints");

        // On-off constraints: link start times to binary variables
        // For each activity i: y_{it} = 1 if and only if S_i <= t < S_i + p_i
        // Skip dummy activities (duration 0)
        System.out.println("Adding on-off constraints...");
        int onOffCount = 0;
        
        for (int i = 0; i < data.numberJob; i++) {
            int duration = data.jobDuration.get(i);
            
            // Skip dummy activities with duration 0
            if (duration == 0) {
                continue;
            }
            
            for (int t = 0; t < data.horizon; t++) {
                if (onOffVars[i][t] == null) continue;
                
                // Use a larger Big-M value
                int M = latestStartTime[i] + duration + data.horizon;
                
                // y_{it} = 1 implies S_i <= t
                // Reformulated as: S_i <= t + M * (1 - y_{it})
                // If y_{it} = 1, then S_i <= t
                // If y_{it} = 0, constraint is relaxed (S_i <= t + M)
                
                GRBLinExpr expr1 = new GRBLinExpr();
                expr1.addTerm(1, startingTimeVars[i]);
                
                GRBLinExpr expr2 = new GRBLinExpr();
                expr2.addConstant(t);
                expr2.addConstant(M);
                expr2.addTerm(-M, onOffVars[i][t]);
                
                model.addConstr(expr1, GRB.LESS_EQUAL, expr2, "on_off_start_" + i + "_" + t);
                
                // y_{it} = 1 implies t < S_i + p_i
                // Reformulated as: t <= S_i + p_i - 1 + M * (1 - y_{it})
                // If y_{it} = 1, then t <= S_i + p_i - 1 (so t < S_i + p_i)
                // If y_{it} = 0, constraint is relaxed (t <= S_i + p_i - 1 + M)
                
                GRBLinExpr expr3 = new GRBLinExpr();
                expr3.addConstant(t);
                
                GRBLinExpr expr4 = new GRBLinExpr();
                expr4.addTerm(1, startingTimeVars[i]);
                expr4.addConstant(duration - 1);
                expr4.addConstant(M);
                expr4.addTerm(-M, onOffVars[i][t]);
                
                model.addConstr(expr3, GRB.LESS_EQUAL, expr4, "on_off_end_" + i + "_" + t);
                
                onOffCount += 2;
            }
            
            // Constraint: sum of y_{it} = p_i (activity is active for exactly p_i time units)
            GRBLinExpr activityDuration = new GRBLinExpr();
            for (int t = 0; t < data.horizon; t++) {
                if (onOffVars[i][t] != null) {
                    activityDuration.addTerm(1, onOffVars[i][t]);
                }
            }
            model.addConstr(activityDuration, GRB.EQUAL, duration, "duration_" + i);
            onOffCount++;
        }
        System.out.println("Added " + onOffCount + " on-off constraints");

        // Resource constraints: sum_i b_{ik} * y_{it} <= B_k for all k, t
        // Skip dummy activities (duration 0) as they don't use resources during execution
        System.out.println("Adding resource constraints...");
        int resourceCount = 0;
        
        for (int k = 0; k < data.resourceCapacity.size(); k++) {
            for (int t = 0; t < data.horizon; t++) {
                GRBLinExpr resourceUsage = new GRBLinExpr();
                
                for (int i = 0; i < data.numberJob; i++) {
                    int duration = data.jobDuration.get(i);
                    int resourceDemand = data.jobResource.get(i).get(k);
                    
                    // Skip dummy activities (duration 0) and activities with no resource demand
                    if (duration > 0 && resourceDemand > 0 && onOffVars[i][t] != null) {
                        resourceUsage.addTerm(resourceDemand, onOffVars[i][t]);
                    }
                }
                
                model.addConstr(resourceUsage, GRB.LESS_EQUAL, data.resourceCapacity.get(k),
                               "resource_" + k + "_time_" + t);
                resourceCount++;
            }
        }
        System.out.println("Added " + resourceCount + " resource constraints");

        // Set dummy start activity constraint: S_0 = 0 (implicit in bounds)
        model.addConstr(startingTimeVars[0], GRB.EQUAL, 0.0, "dummy_start");

        // Apply heuristic solution as warm start
        //List<Integer> heuristicSolution = HeuristicSerialSGS.serialSGS(data);
        //applySolutionWithGurobi(model, startingTimeVars, onOffVars, data.numberJob, data.jobDuration, heuristicSolution);

        return model;
    }

    private static void applySolutionWithGurobi(GRBModel model, GRBVar[] startingTimeVars, GRBVar[][] onOffVars,
                                                int numberJob, List<Integer> jobDuration, List<Integer> startTimes) throws GRBException {
        model.update();
        
        System.out.println("Applying warm start solution...");
        
        // Set start time variable hints
        for (int i = 0; i < numberJob; i++) {
            double startTime = startTimes.get(i);
            startingTimeVars[i].set(GRB.DoubleAttr.Start, startTime);
            
            int duration = jobDuration.get(i);
            
            // Skip on-off variables for dummy activities (duration 0)
            if (duration == 0) {
                continue;
            }
            
            // Set on-off variable hints more carefully
            for (int t = 0; t < onOffVars[i].length; t++) {
                if (onOffVars[i][t] != null) {
                    // Activity i is active at time t if startTime <= t < startTime + duration
                    // Make sure we use integer start times for consistency
                    int intStartTime = (int) Math.round(startTime);
                    double value = (t >= intStartTime && t < intStartTime + duration) ? 1.0 : 0.0;
                    onOffVars[i][t].set(GRB.DoubleAttr.Start, value);
                }
            }
            
            if (i < 5) { // Debug first few activities
                System.out.println("Activity " + i + ": start=" + startTime + ", duration=" + duration);
            }
        }
        
        model.update();
        System.out.println("Warm start applied successfully.");
    }

    private static io.ScheduleDoubleResult fillListsToReturn(GRBModel model, GRBVar[] startingTimeVars,
                                                                List<Integer> jobDuration, int numJob) throws GRBException {
        List<Double> start = new ArrayList<>(numJob);
        List<Double> finish = new ArrayList<>(numJob);

        for (int i = 0; i < numJob; i++) {
            double startTime = startingTimeVars[i].get(GRB.DoubleAttr.X);
            double finishTime = startTime + jobDuration.get(i);
            
            // Round to avoid floating-point precision issues
            if (Math.abs(startTime - Math.round(startTime)) < 1e-6) {
                startTime = Math.round(startTime);
            }
            if (Math.abs(finishTime - Math.round(finishTime)) < 1e-6) {
                finishTime = Math.round(finishTime);
            }
            
            start.add(startTime);
            finish.add(finishTime);
        }

        return new Result.ScheduleDoubleResult(start, finish);
    }
}
