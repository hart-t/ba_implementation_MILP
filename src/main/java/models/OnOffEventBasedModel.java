package models;

import com.gurobi.gurobi.*;

import io.JobDataInstance;
import interfaces.ModelInterface;
import interfaces.ModelSolutionInterface;
import interfaces.CompletionMethodInterface;
import solutionBuilder.BuildOnOffEventSolution;
import utility.DeleteDummyJobs;

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

public class OnOffEventBasedModel implements ModelInterface {
    private final CompletionMethodInterface completionMethod;

    public OnOffEventBasedModel() {
        // Constructor
        this.completionMethod = new BuildOnOffEventSolution();
    }

    @Override
    public CompletionMethodInterface getCompletionMethod() {
        return completionMethod;
    }
    
    public GRBModel completeModel(ModelSolutionInterface initialSolution, JobDataInstance data) {

        try {
            GRBModel model = initialSolution.getModel();

            // Cast to access the Variables
            GRBVar makespanVar;
            GRBVar[] startOfEventEVars;
            GRBVar[][] jobActiveAtEventVars;
            int[] earliestStartTime;
            int[] latestStartTime;
            if (initialSolution instanceof OnOffEventBasedModelSolution) {
                makespanVar = ((OnOffEventBasedModelSolution) initialSolution)
                        .getMakespanVar();
                startOfEventEVars = ((OnOffEventBasedModelSolution) initialSolution)
                        .getStartOfEventEVars();
                jobActiveAtEventVars = ((OnOffEventBasedModelSolution) initialSolution)
                        .getJobActiveAtEventVars();
                earliestStartTime = ((OnOffEventBasedModelSolution) initialSolution)
                        .getEarliestLatestStartTimes()[0];
                latestStartTime = ((OnOffEventBasedModelSolution) initialSolution)
                        .getEarliestLatestStartTimes()[1];
            } else {
                throw new IllegalArgumentException("Expected DiscreteTimeModelSolution but got " + 
                                                 initialSolution.getClass().getSimpleName());
            }

            // delete dummy jobs from data and earliestLatestStartTimes
            JobDataInstance noDummyData = DeleteDummyJobs.deleteDummyJobs(data);
            int[] newEarliestStartTime = new int[noDummyData.numberJob];
            int[] newLatestStartTime = new int[noDummyData.numberJob];
            for (int i = 1; i < earliestStartTime.length - 1; i++) {
                newEarliestStartTime[i - 1] = earliestStartTime[i];
                newLatestStartTime[i - 1] = latestStartTime[i];
            }
            
            // (41) We also use one single extra continuous variable Cmax to represent the makespan of the 
            // schedule. Set the objective to minimize Cmax
            GRBLinExpr obj = new GRBLinExpr();
            obj.addTerm(1, makespanVar);
            model.setObjective(obj, GRB.MINIMIZE);

            // (42) Constraints (42) ensure that each activity is processed at least once during the project.
            for (int i = 0; i < noDummyData.numberJob; i++) {
                GRBLinExpr expr1 = new GRBLinExpr();
                for (int e = 0; e < startOfEventEVars.length; e++) {
                    expr1.addTerm(1, jobActiveAtEventVars[i][e]);
                }
                model.addConstr(expr1, GRB.GREATER_EQUAL, 1, "all " + i + "greater equal 1 (42)");
            }

            // (43) Constraints (43) link the makespan to the event dates: Cmax >= te + (zie - zi,e-1)pi
            // for all e in E, all i in A
            for (int i = 0; i < noDummyData.numberJob; i++) {
                // start at e = 1 to ensure e-1 >= 0
                for (int e = 1; e < startOfEventEVars.length; e++) {
                    GRBLinExpr expr1 = new GRBLinExpr();
                    GRBLinExpr expr2 = new GRBLinExpr();
                    
                    expr1.addTerm(1, makespanVar);
                    
                    expr2.addTerm(1, startOfEventEVars[e]);
                    expr2.addTerm(noDummyData.jobDuration.get(i), jobActiveAtEventVars[i][e]);
                    expr2.addTerm(- noDummyData.jobDuration.get(i), jobActiveAtEventVars[i][e - 1]);
                    
                    model.addConstr(expr1, GRB.GREATER_EQUAL, expr2 , "makespan_constraint_" + i + "_" + e + "(43)");
                }
            }

            // (44), (45) Constraints (44), (45) ensure event sequencing.
            // TODO im paper steht "für alle e, ausgenommen n-1", es müsste "ausgenommen n" sein da man 
            // sonst auf event n+1 zugreifen würde
            GRBLinExpr expr01 = new GRBLinExpr();
            expr01.addTerm(1, startOfEventEVars[0]);
            model.addConstr(expr01, GRB.EQUAL, 0, "event 0 starts at time dated 0 (44)");

            // skip event n
            for (int e = 0; e < startOfEventEVars.length - 1; e++) {
                GRBLinExpr expr1 = new GRBLinExpr();
                GRBLinExpr expr2 = new GRBLinExpr();

                expr1.addTerm(1, startOfEventEVars[e + 1]);
                expr2.addTerm(1, startOfEventEVars[e]);

                model.addConstr(expr1, GRB.GREATER_EQUAL, expr2, expr1 + " starts after " + expr2 + "(45)");
            }

            // (46) Constraints (46) link the binary optimization variables zie to the continuous optimization
            //  variables te, and ensure that, if activity i starts immediately after event e and ends at event 
            // f, then the date of event f is at least equal to the date of event e plus the processing time of
            // activity i (tf >= te + pi)
            // start at 1 or 0 TODO
            for (int e = 1; e < startOfEventEVars.length; e++) {
                for (int f = 1; f < startOfEventEVars.length; f++) {
                    if (f <= e) {
                        continue;
                    }
                    for (int i = 0; i < noDummyData.numberJob; i++) {
                        GRBLinExpr expr1 = new GRBLinExpr();
                        GRBLinExpr expr2 = new GRBLinExpr();

                        expr1.addTerm(1, startOfEventEVars[f]);

                        expr2.addTerm(1, startOfEventEVars[e]);
                        expr2.addTerm(noDummyData.jobDuration.get(i), jobActiveAtEventVars[i][e]);
                        expr2.addTerm(- noDummyData.jobDuration.get(i), jobActiveAtEventVars[i][e - 1]);
                        expr2.addTerm(- noDummyData.jobDuration.get(i), jobActiveAtEventVars[i][f]);
                        expr2.addTerm(noDummyData.jobDuration.get(i), jobActiveAtEventVars[i][f - 1]);
                        expr2.addConstant(- noDummyData.jobDuration.get(i));

                        model.addConstr(expr1, GRB.GREATER_EQUAL, expr2, expr1 + " greater equal " + expr2 + "(46)");
                    }
                }
            }

            // (47) if activity i starts at event e, then i cannot be processed before e.
            // TODO missing "für alle i element A"
            // Constraints (47), (48), called contiguity constraints, ensure non-preemption 
            // (the events after which a given activity is being processed are adjacent).
            for (int i = 0; i < noDummyData.numberJob; i++) {
                for (int e = 1; e < startOfEventEVars.length; e++) {
                    GRBLinExpr expr1 = new GRBLinExpr();
                    GRBLinExpr expr2 = new GRBLinExpr();
                    for (int ee = 0; ee < e - 1; ee++) {
                        expr1.addTerm(1, jobActiveAtEventVars[i][ee]);
                    }

                    expr2.addConstant(e);
                    expr2.addTerm(- e, jobActiveAtEventVars[i][e]);
                    expr2.addTerm(e, jobActiveAtEventVars[i][e - 1]);

                    model.addConstr(expr1, GRB.LESS_EQUAL, expr2, expr1 + " less equal " + expr2 + " (47)");
                }
            }

            // (48) 
            for (int i = 0; i < noDummyData.numberJob; i++) {
                for (int e = 1; e < startOfEventEVars.length; e++) {
                    GRBLinExpr expr1 = new GRBLinExpr();
                    GRBLinExpr expr2 = new GRBLinExpr();
                    // Fix: ensure ee doesn't go out of bounds
                    for (int ee = e; ee < Math.min(noDummyData.numberJob, startOfEventEVars.length); ee++) {
                        expr1.addTerm(1, jobActiveAtEventVars[i][ee]);
                    }

                    expr2.addConstant((noDummyData.numberJob - e));
                    expr2.addTerm((noDummyData.numberJob - e), jobActiveAtEventVars[i][e]);
                    expr2.addTerm(- (noDummyData.numberJob - e), jobActiveAtEventVars[i][e - 1]);

                    model.addConstr(expr1, GRB.LESS_EQUAL, expr2, expr1 + " less equal " + expr2 + " (48)");
                }
            }

            // Constraints (49) describe each precedence constraint (i,j) element E(precedence graph).
            // According to Koné et al. 2011, the precedence constraint should ensure that 
            // if job i starts at event e, then its predecessor j must have finished before event e.
            // 
            // CORRECT IMPLEMENTATION: For event-based models, we need to ensure that
            // if job i starts at event e, then predecessor j must have completed its processing
            // 
            // The correct constraint is: For each job i and predecessor j, for each event e:
            // t_e >= t_e' + p_j * (z_i,e - z_i,e-1) for all e' where predecessor j finishes
            // 
            // But in practice, we can use a simpler formulation:
            // For each event e, if job i starts at event e (z_i,e - z_i,e-1 = 1), 
            // then predecessor j cannot be active at event e or later
            System.out.println("Adding precedence constraints (49)...");
            System.out.println("DEBUG: Checking precedence relationships for jobs 7 and 8...");
            for (int i = 0; i < noDummyData.numberJob; i++) {
                if (i == 7 || i == 8) {
                    System.out.println("Job " + i + " has " + noDummyData.jobPredecessors.get(i).size() + " predecessors: " + noDummyData.jobPredecessors.get(i));
                }
                for (int j = 0; j < noDummyData.jobPredecessors.get(i).size(); j++) {
                    int predecessor = noDummyData.jobPredecessors.get(i).get(j);
                    // Adjust for 0-based indexing if needed
                    if (predecessor > 0) {
                        predecessor = predecessor - 1; // Convert to 0-based indexing
                    }

                    if (i == 8) {
                        System.out.println(j + "  -------------------" + predecessor);
                    }
                    
                    if (i <= 8) { // Print debug for first few jobs including job 8
                        System.out.println("Job " + i + " has predecessor " + predecessor);
                    }
                    
                    // Alternative formulation: precedence through event ordering
                    // For each event e, if job i starts at event e, then predecessor j must not be active at e or later
                    for (int e = 0; e < startOfEventEVars.length; e++) { // Changed: start from event 0
                        GRBLinExpr leftSide = new GRBLinExpr();
                        GRBLinExpr rightSide = new GRBLinExpr();
                        
                        if (e == 0) {
                            // Special case for event 0: if job i is active at event 0, predecessor must not be active at any event
                            leftSide.addTerm(1, jobActiveAtEventVars[i][e]);
                            
                            // Right side: predecessor j should not be active at any event if job i is active at event 0
                            for (int ee = 0; ee < startOfEventEVars.length; ee++) {
                                rightSide.addTerm(1, jobActiveAtEventVars[predecessor][ee]);
                            }
                        } else {
                            // For e >= 1: job i starts at event e
                            leftSide.addTerm(1, jobActiveAtEventVars[i][e]);
                            leftSide.addTerm(-1, jobActiveAtEventVars[i][e-1]);
                            
                            // Right side: predecessor j should not be active at event e or later
                            for (int ee = e; ee < startOfEventEVars.length; ee++) {
                                rightSide.addTerm(1, jobActiveAtEventVars[predecessor][ee]);
                            }
                        }
                        
                        // Constraint: leftSide + rightSide <= 1
                        // This ensures proper precedence ordering
                        GRBLinExpr totalExpr = new GRBLinExpr();
                        totalExpr.add(leftSide);
                        totalExpr.add(rightSide);

                        String constraintName = "precedence_job_" + i + "_pred_" + predecessor + "_event_" + e + "_(49)";
                        model.addConstr(totalExpr, GRB.LESS_EQUAL, 1, constraintName);
                        
                        if ((e <= 3 && i <= 3) || (i == 7 && e == 0) || (i == 8 && e == 0)) { // Add debug for jobs 7,8 at event 0
                            System.out.println("Added constraint: " + constraintName);
                            if (e == 0) {
                                System.out.println("  If job " + i + " is active at event 0, then predecessor " + predecessor + " cannot be active at any event");
                            } else {
                                System.out.println("  If job " + i + " starts at event " + e + ", then predecessor " + predecessor + " cannot be active at event " + e + " or later");
                            }
                        }
                    }
                }
            }
            System.out.println("Finished adding precedence constraints (49).");

            // (50) the resource constraints limiting the total demand of activities in process at each event.
            for (int e = 0; e < startOfEventEVars.length; e++) {
                for (int k = 0; k < noDummyData.resourceCapacity.size(); k++) {
                    GRBLinExpr expr1 = new GRBLinExpr();
                    // TODO wieder nur bis n - 1?
                    for (int i = 0; i < noDummyData.numberJob; i++) {
                        expr1.addTerm(noDummyData.jobResource.get(i).get(k), jobActiveAtEventVars[i][e]);
                    }
                    model.addConstr(expr1, GRB.LESS_EQUAL, noDummyData.resourceCapacity.get(k),
                        "resource_" + k + "_event_" + e + " (50)");
                }
            }

            // (51) Constraints (51) and (52) set the start time of any activity i between its earliest start 
            // time ESi and its latest start time LSi.
            for (int e = 1; e < startOfEventEVars.length; e++) {
                for (int i = 0; i < noDummyData.numberJob; i++) {
                    GRBLinExpr expr1 = new GRBLinExpr();
                    GRBLinExpr expr2 = new GRBLinExpr();
                    GRBLinExpr expr3 = new GRBLinExpr();

                    expr1.addTerm(newEarliestStartTime[i], jobActiveAtEventVars[i][e]);

                    expr2.addTerm(1, startOfEventEVars[e]);

                    expr3.addTerm(newLatestStartTime[i], jobActiveAtEventVars[i][e]);
                    expr3.addTerm(- newLatestStartTime[i], jobActiveAtEventVars[i][e - 1]);
                    expr3.addConstant(newLatestStartTime[newLatestStartTime.length - 1]);
                    expr3.addTerm(- newLatestStartTime[newLatestStartTime.length - 1], jobActiveAtEventVars[i][e]);
                    expr3.addTerm(newLatestStartTime[newLatestStartTime.length - 1], jobActiveAtEventVars[i][e - 1]);


                    model.addConstr(expr1, GRB.LESS_EQUAL, expr2, expr1 + " less equal " + expr2 + " (51)");
                    model.addConstr(expr2, GRB.LESS_EQUAL, expr3, expr2 + " less equal " + expr3 + " (51)");   
                }
            }

            /*
             * // (52) TODO für alle e und für alle i?
            // ESi <= makespanVar <= LSi
            GRBLinExpr expr02 = new GRBLinExpr();
            expr02.addTerm(1, makespanVar);
            model.addConstr(newEarliestStartTime[newEarliestStartTime.length - 1], GRB.LESS_EQUAL, expr02,
                "ESn+1 less equal makespanVar (52)");
            model.addConstr(expr02, GRB.LESS_EQUAL, newLatestStartTime[newLatestStartTime.length - 1],
                "makespanVar less equal LSn+1 (52)");
             */

            // (53) te >= 0 for all e in E, unnessesary as we set the lower bound to 0 when creating the 
            // variable
            // (54) zie element {0,1} for all i in A, e in E, unnessesary as we set the type to binary when
            // creating the variable

            // Write model to file for debugging
            try {
                model.write("debug_model.lp");
                System.out.println("Model written to debug_model.lp for inspection");
            } catch (GRBException ex) {
                System.err.println("Could not write model to file: " + ex.getMessage());
            }

            return model;

        } catch (Exception e) {
            System.err.println("Error while solving the model: " + e.getMessage());
            return null;
        }
    }

    @Override
    public boolean usesDummyJobs() {
        // This model does not use dummy jobs
        return false;
    }

    public class OnOffEventBasedModelSolution implements ModelSolutionInterface {
        GRBVar makespanVar;
        GRBVar[] startOfEventEVars;
        GRBVar[][] jobActiveAtEventVars;

        GRBModel model;
        int[][] earliestLatestStartTimes;

        public OnOffEventBasedModelSolution(GRBVar makespanVar, GRBVar[] startOfEventE, GRBVar[][] jobActiveAtEvent, 
                                            GRBModel model, int[][] earliestLatestStartTimes) {
            this.makespanVar = makespanVar;
            this.startOfEventEVars = startOfEventE;
            this.jobActiveAtEventVars = jobActiveAtEvent;
            this.earliestLatestStartTimes = earliestLatestStartTimes;
            this.model = model;
        }

        @Override
        public GRBModel getModel() {
            return model;
        }

        public int[][] getEarliestLatestStartTimes() {
            return earliestLatestStartTimes;
        }

        public GRBVar getMakespanVar() {
            return makespanVar;
        }

        public GRBVar[] getStartOfEventEVars() {
            return startOfEventEVars;
        }

        public GRBVar[][] getJobActiveAtEventVars() {
            return jobActiveAtEventVars;
        }
    }

    @Override
    public int[][] getStartAndFinishTimes(GRBModel model, JobDataInstance data) {
        try {
            // This model doesn't use dummy jobs, so we need to adjust for the original data
            JobDataInstance noDummyData = DeleteDummyJobs.deleteDummyJobs(data);
            
            int[] startTimes = new int[noDummyData.numberJob];
            int[] finishTimes = new int[noDummyData.numberJob];
            
            // Initialize start times to -1 (not found)
            for (int i = 0; i < noDummyData.numberJob; i++) {
                startTimes[i] = -1;
            }
            
            // Extract event times
            double[] eventTimes = new double[noDummyData.numberJob];
            for (int e = 0; e < noDummyData.numberJob; e++) {
                GRBVar eventVar = model.getVarByName("startOfEvent[" + e + "]");
                if (eventVar != null) {
                    eventTimes[e] = eventVar.get(GRB.DoubleAttr.X);
                }
            }
            
            // Find start times by checking when jobs become active
            for (int i = 0; i < noDummyData.numberJob; i++) {
                for (int e = 1; e < noDummyData.numberJob; e++) {
                    try {
                        GRBVar currentVar = model.getVarByName("jobActiveAtEvent[" + i + "][" + e + "]");
                        GRBVar previousVar = model.getVarByName("jobActiveAtEvent[" + i + "][" + (e-1) + "]");
                        
                        if (currentVar != null && previousVar != null) {
                            double currentValue = currentVar.get(GRB.DoubleAttr.X);
                            double previousValue = previousVar.get(GRB.DoubleAttr.X);
                            
                            // Job starts at event e if it becomes active (transitions from 0 to 1)
                            if (currentValue > 0.5 && previousValue < 0.5) {
                                startTimes[i] = (int) Math.round(eventTimes[e]);
                                break;
                            }
                        }
                    } catch (GRBException ex) {
                        // Variable doesn't exist, continue
                    }
                }
                
                // If job is active at event 0, it starts at time 0
                if (startTimes[i] == -1) {
                    try {
                        GRBVar var = model.getVarByName("jobActiveAtEvent[" + i + "][0]");
                        if (var != null && var.get(GRB.DoubleAttr.X) > 0.5) {
                            startTimes[i] = 0;
                        }
                    } catch (GRBException ex) {
                        // Variable doesn't exist, continue
                    }
                }
            }
            
            // Calculate finish times: startTime + duration
            for (int i = 0; i < noDummyData.numberJob; i++) {
                if (startTimes[i] >= 0) {
                    finishTimes[i] = startTimes[i] + noDummyData.jobDuration.get(i);
                } else {
                    finishTimes[i] = -1; // Job not scheduled
                }
            }
            
            return new int[][]{startTimes, finishTimes};
            
        } catch (GRBException e) {
            e.printStackTrace();
        }
        return null;
    }
}
