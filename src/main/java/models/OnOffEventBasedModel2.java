package models;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.gurobi.gurobi.GRB;
import com.gurobi.gurobi.GRBExpr;
import com.gurobi.gurobi.GRBLinExpr;
import com.gurobi.gurobi.GRBModel;
import com.gurobi.gurobi.GRBVar;

import io.FileReader;
import io.JobDataInstance;
import io.Result;
import interfaces.ModelInterface;
import interfaces.ModelSolutionInterface;

/**
 * On off Event-Based Model 2
 *
 * https://www.sciencedirect.com/science/article/pii/S0305054809003360
 *      on off event-based model
 *
 *      public final int numberJob;
 *      public final int horizon;
 *      public final List<Integer> jobNumSuccessors;
 *      public final List<List<Integer>> jobSuccessors;
 *      public final List<List<Integer>> jobPredecessors;
 *      public final List<Integer> jobDuration;
 *      public final List<List<Integer>> jobResource;
 *      public final List<Integer> resourceCapacity;
 * 
 *      In this model, the number of events is exactly equal to the number of activities, n.
 *      event-based formulations do not involve the use of dummy activities.
 * */

public class OnOffEventBasedModel2 implements ModelInterface {
    
    public Result solve(ModelSolutionInterface initialSolution, JobDataInstance data) {
        try {
            // TODO DELETE DUMMYS
            GRBModel model = initialSolution.getModel();

            // generate TE matrix, This means that if there is a path from activity i to j in the precedence graph
            // (even if not directly), then (i,j) element TE.
            int[][] teMatrix = computeTEMatrix(data.numberJob, data.jobSuccessors, data.jobDuration);

            /*for (int[] row : teMatrix) {
                System.out.println(Arrays.toString(row));
            }*/

            
            int[][] startTimes = generateEarliestAndLatestStartTimes(data.jobPredecessors, data.jobDuration, data.horizon);
            int[] earliestStartTime = startTimes[0];
            int[] latestStartTime = startTimes[1];

            // add zie
            GRBVar[][] jobActiveAtEvent = new GRBVar[data.numberJob][data.numberJob];
            for (int i = 0; i < data.numberJob; i++) {
                for (int e = 0; e < data.numberJob; e++) {
                    jobActiveAtEvent[i][e] = model.addVar(0.0, data.horizon, 0.0, GRB.CONTINUOUS, "startingTime[" +
                        i + "]");
                }
            }

            // represents the date of the start of the event e (te)
            GRBVar[] startOfEventE = new GRBVar[data.numberJob];
            for (int e = 0; e < startOfEventE.length; e++) {
                startOfEventE[e] = model.addVar(0.0, data.horizon, 0.0, GRB.CONTINUOUS, "startOfEvent[" + e + "]");
            }

            // (41) We also use one single extra continuous variable Cmax to represent the makespan of the 
            // schedule. Set the objective to minimize Cmax
            GRBVar makespanVar = model.addVar(0.0, data.horizon, 0.0, GRB.CONTINUOUS, "makespan");
            GRBLinExpr obj = new GRBLinExpr();
            obj.addTerm(1, makespanVar);
            model.setObjective(obj, GRB.MINIMIZE);

            // (42) Constraints (42) ensure that each activity is processed at least once during the project.
            for (int i = 0; i < data.numberJob; i++) {
                GRBLinExpr expr1 = new GRBLinExpr();
                for (int e = 0; e < startOfEventE.length; e++) {
                    expr1.addTerm(1, jobActiveAtEvent[i][e]);
                }
                model.addConstr(expr1, GRB.GREATER_EQUAL, 1, expr1 + "greater equal 1 (42)");
            }

            // (43) Constraints (43) link the makespan to the event dates: Cmax >= te + (zie - zi,e-1)pi
            // for all e in E, all i in A
            for (int i = 0; i < data.numberJob; i++) {
                // start at e = 1
                for (int e = 1; e < startOfEventE.length - 1; e++) {
                    GRBLinExpr expr1 = new GRBLinExpr();
                    GRBLinExpr expr2 = new GRBLinExpr();
                    
                    expr1.addTerm(1, makespanVar);
                    
                    expr2.addTerm(1, startOfEventE[e]);
                    expr2.addTerm(data.jobDuration.get(i), jobActiveAtEvent[i][e]);
                    expr2.addTerm(- data.jobDuration.get(i), jobActiveAtEvent[i][e - 1]);
                    
                    model.addConstr(expr1, GRB.GREATER_EQUAL, expr2 , "makespan_constraint_" + i + "_" + e + "(43)");
                }
            }

            // (44), (45) Constraints (44), (45) ensure event sequencing.
            // TODO im paper steht "für alle e, ausgenommen n-1", es müsste "ausgenommen n" sein da man 
            // sonst auf event n+1 zugreifen würde
            GRBLinExpr expr01 = new GRBLinExpr();
            expr01.addTerm(1, startOfEventE[0]);
            model.addConstr(expr01, GRB.EQUAL, 0, "event 0 starts at time dated 0 (44)");

            // skip event n
            for (int e = 0; e < startOfEventE.length - 1; e++) {
                GRBLinExpr expr1 = new GRBLinExpr();
                GRBLinExpr expr2 = new GRBLinExpr();

                expr1.addTerm(1, startOfEventE[e + 1]);
                expr2.addTerm(1, startOfEventE[e]);

                model.addConstr(expr1, GRB.GREATER_EQUAL, expr2, expr1 + " starts after " + expr2 + "(45)");
            }

            // (46) Constraints (46) link the binary optimization variables zie to the continuous optimization
            //  variables te, and ensure that, if activity i starts immediately after event e and ends at event 
            // f, then the date of event f is at least equal to the date of event e plus the processing time of
            // activity i (tf >= te + pi)
            for (int e = 0; e < startOfEventE.length; e++) {
                for (int f = 0; f < startOfEventE.length; f++) {
                    if (f <= e) {
                        continue;
                    }
                    for (int i = 0; i < data.numberJob; i++) {
                        GRBLinExpr expr1 = new GRBLinExpr();
                        GRBLinExpr expr2 = new GRBLinExpr();

                        expr1.addTerm(1, startOfEventE[f]);

                        expr2.addTerm(1, startOfEventE[e]);
                        expr2.addTerm(data.jobDuration.get(i), jobActiveAtEvent[i][e]);
                        expr2.addTerm(- data.jobDuration.get(i), jobActiveAtEvent[i][e - 1]);
                        expr2.addTerm(- data.jobDuration.get(i), jobActiveAtEvent[i][f]);
                        expr2.addTerm(data.jobDuration.get(i), jobActiveAtEvent[i][f - 1]);
                        expr2.addConstant(- data.jobDuration.get(i));

                        model.addConstr(expr1, GRB.GREATER_EQUAL, expr2, expr1 + " greater equal " + expr2 + "(46)");
                    }
                }
            }

            // (47) if activity i starts at event e, then i cannot be processed before e.
            // TODO missing "für alle i element A"
            // Constraints (47), (48), called contiguity constraints, ensure non-preemption 
            // (the events after which a given activity is being processed are adjacent).
            for (int i = 0; i < data.numberJob; i++) {
                for (int e = 1; e < startOfEventE.length; e++) {
                    GRBLinExpr expr1 = new GRBLinExpr();
                    GRBLinExpr expr2 = new GRBLinExpr();
                    for (int ee = 0; ee < e - 1; ee++) {
                        expr1.addTerm(1, jobActiveAtEvent[i][ee]);
                    }

                    expr2.addConstant(e);
                    expr2.addTerm(- e, jobActiveAtEvent[i][e]);
                    expr2.addTerm(e, jobActiveAtEvent[i][e]);

                    model.addConstr(expr1, GRB.LESS_EQUAL, expr2, expr1 + " less equal " + expr2 + " (47)");
                }
            }

            // (48) 
            for (int i = 0; i < data.numberJob; i++) {
                
            }
            



        } catch (Exception e) {
            System.err.println("Error while solving the model: " + e.getMessage());
            return null;
        }
    }
}
