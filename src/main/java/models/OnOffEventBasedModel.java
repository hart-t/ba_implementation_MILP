package models;

import com.gurobi.gurobi.*;

import io.JobDataInstance;
import interfaces.ModelInterface;
import interfaces.ModelSolutionInterface;
import interfaces.CompletionMethodInterface;
import solutionBuilder.BuildOnOffEventSolution;
import utility.DeleteDummyJobs;
import modelSolutions.OnOffEventBasedModelSolution;

/**
 * On off Event-Based Model
 *
 * https://www.sciencedirect.com/science/article/pii/S0305054809003360
 *      on off event-based model
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
            GRBVar[] startOfEventVars;
            GRBVar[][] jobActiveAtEventVars;
            int[] earliestStartTime;
            int[] latestStartTime;
            if (initialSolution instanceof OnOffEventBasedModelSolution) {
                makespanVar = ((OnOffEventBasedModelSolution) initialSolution)
                        .getMakespanVar();
                startOfEventVars = ((OnOffEventBasedModelSolution) initialSolution)
                        .getStartOfEventEVars();
                jobActiveAtEventVars = ((OnOffEventBasedModelSolution) initialSolution)
                        .getJobActiveAtEventVars();
                earliestStartTime = ((OnOffEventBasedModelSolution) initialSolution)
                        .getEarliestLatestStartTimes()[0];
                latestStartTime = ((OnOffEventBasedModelSolution) initialSolution)
                        .getEarliestLatestStartTimes()[1];
            } else {
                throw new IllegalArgumentException("Expected OnOffEventBasedModelSolution but got " + 
                                                 initialSolution.getClass().getSimpleName());
            }

            // delete dummy jobs from data and earliestLatestStartTimes
            data = DeleteDummyJobs.deleteDummyJobs(data);
            
            // (41) We also use one single extra continuous variable Cmax to represent the makespan of the 
            // schedule. Set the objective to minimize Cmax
            GRBLinExpr obj = new GRBLinExpr();
            obj.addTerm(1, makespanVar);
            model.setObjective(obj, GRB.MINIMIZE);

            // (42) Constraints (42) ensure that each activity is processed at least once during the project.
            for (int i = 0; i < data.numberJob; i++) {
                GRBLinExpr expr1 = new GRBLinExpr();
                for (int e = 0; e < startOfEventVars.length; e++) {
                    expr1.addTerm(1, jobActiveAtEventVars[i][e]);
                }
                model.addConstr(expr1, GRB.GREATER_EQUAL, 1, "(42)");

            }

            // (43) Constraints (43) link the makespan to the event dates: Cmax >= te + (zie - zi,e-1)pi
            // for all e in E, all i in A
            for (int i = 0; i < data.numberJob; i++) {
                // start at e = 1 to ensure e-1 >= 0
                for (int e = 0; e < startOfEventVars.length; e++) {
                    GRBLinExpr expr1 = new GRBLinExpr();
                    GRBLinExpr expr2 = new GRBLinExpr();
                    
                    expr1.addTerm(1, makespanVar);
                    
                    expr2.addTerm(1, startOfEventVars[e]);
                    expr2.addTerm(data.jobDuration.get(i), jobActiveAtEventVars[i][e]);

                    if (e > 0) {
                        expr2.addTerm(- data.jobDuration.get(i), jobActiveAtEventVars[i][e - 1]);
                    }
                    
                    model.addConstr(expr1, GRB.GREATER_EQUAL, expr2 , "makespan_constraint_" + i + "_" + e + "(43)");
                }
            }

            // (44), (45) Constraints (44), (45) ensure event sequencing.
            GRBLinExpr expr01 = new GRBLinExpr();
            expr01.addTerm(1, startOfEventVars[0]);
            model.addConstr(expr01, GRB.EQUAL, 0, "event_0_starts_at_time_dated_0_(44)");

            // skip event n
            for (int e = 0; e < startOfEventVars.length - 1; e++) {
                GRBLinExpr expr1 = new GRBLinExpr();
                GRBLinExpr expr2 = new GRBLinExpr();

                expr1.addTerm(1, startOfEventVars[e + 1]);
                expr2.addTerm(1, startOfEventVars[e]);

                model.addConstr(expr1, GRB.GREATER_EQUAL, expr2, "event_" + (e+1) + "_starts_after_event_" + e + "_(45)");
            }

            // (46) Constraints (46) link the binary optimization variables zie to the continuous optimization
            //  variables te, and ensure that, if activity i starts immediately after event e and ends at event 
            // f, then the date of event f is at least equal to the date of event e plus the processing time of
            // activity i (tf >= te + pi)

            for (int i = 0; i < data.numberJob; i++) {
                for (int e = 0; e < startOfEventVars.length; e++) {
                    for (int f = e + 1; f < startOfEventVars.length; f++) {
                        GRBLinExpr leftSide = new GRBLinExpr();
                        GRBLinExpr rightSide = new GRBLinExpr();

                        leftSide.addTerm(1, startOfEventVars[f]);

                        rightSide.addTerm(1, startOfEventVars[e]);
                        rightSide.addTerm(data.jobDuration.get(i), jobActiveAtEventVars[i][e]);
                        if (e > 0) {
                            rightSide.addTerm(- data.jobDuration.get(i), jobActiveAtEventVars[i][e - 1]);
                        }
                        rightSide.addTerm(- data.jobDuration.get(i), jobActiveAtEventVars[i][f]);
                        rightSide.addTerm(data.jobDuration.get(i), jobActiveAtEventVars[i][f - 1]);
                        rightSide.addConstant(- data.jobDuration.get(i));

                        model.addConstr(leftSide, GRB.GREATER_EQUAL, rightSide, 
                            "constraint_" + i + "_" + e + "_" + f + "_(46)");
                    }
                }
            }
            
            // (47) if activity i starts at event e, then i cannot be processed before e.
            // TODO missing "für alle i element A"
            // Constraints (47), (48), called contiguity constraints, ensure non-preemption 
            // (the events after which a given activity is being processed are adjacent).
            for (int i = 0; i < data.numberJob; i++) {
                for (int e = 1; e < startOfEventVars.length; e++) {
                    GRBLinExpr expr1 = new GRBLinExpr();
                    GRBLinExpr expr2 = new GRBLinExpr();
                    for (int ee = 0; ee < e - 1; ee++) {
                        expr1.addTerm(1, jobActiveAtEventVars[i][ee]);
                    }

                    expr2.addConstant(e);
                    expr2.addTerm(- e, jobActiveAtEventVars[i][e]);
                    expr2.addTerm(e, jobActiveAtEventVars[i][e - 1]);

                    model.addConstr(expr1, GRB.LESS_EQUAL, expr2, "contiguity_constraint_" + i + "_" + e + "_(47)");
                }
            }

            // (48) 
            for (int i = 0; i < data.numberJob; i++) {
                for (int e = 1; e < startOfEventVars.length; e++) {
                    GRBLinExpr expr1 = new GRBLinExpr();
                    GRBLinExpr expr2 = new GRBLinExpr();
                    // Fix: ensure ee doesn't go out of bounds
                    for (int ee = e; ee < Math.min(data.numberJob, startOfEventVars.length); ee++) {
                        expr1.addTerm(1, jobActiveAtEventVars[i][ee]);
                    }

                    expr2.addConstant((data.numberJob - e));
                    expr2.addTerm((data.numberJob - e), jobActiveAtEventVars[i][e]);
                    expr2.addTerm(- (data.numberJob - e), jobActiveAtEventVars[i][e - 1]);

                    model.addConstr(expr1, GRB.LESS_EQUAL, expr2, "contiguity_constraint_" + i + "_" + e + "_(48)");
                }
            }

            // Constraints (49) describe each precedence constraint (i,j) element E(precedence graph).
            for (int jobIndex = 0; jobIndex < data.numberJob; jobIndex++) {
                for (int predecessor : data.jobPredecessors.get(jobIndex)) {

                    for (int eventIndex = 0; eventIndex < startOfEventVars.length; eventIndex++) {
                        GRBLinExpr leftSide = new GRBLinExpr();
                        GRBLinExpr rightSide = new GRBLinExpr();

                        leftSide.addTerm(1, jobActiveAtEventVars[predecessor][eventIndex]);

                        for (int ee = 0; ee <= eventIndex; ee++) {
                            leftSide.addTerm(1, jobActiveAtEventVars[jobIndex][ee]);
                        }

                        rightSide.addConstant(1.0);
                        rightSide.addConstant(eventIndex);
                        rightSide.addTerm(-eventIndex, jobActiveAtEventVars[predecessor][eventIndex]);

                        model.addConstr(leftSide, GRB.LESS_EQUAL, rightSide, "Job_" + jobIndex + "_Predecessor_" 
                            + predecessor + "_Event_" + eventIndex + "_(49)");
                    }
                }
            }

            // (50) the resource constraints limiting the total demand of activities in process at each event.
            for (int e = 0; e < startOfEventVars.length; e++) {
                for (int k = 0; k < data.resourceCapacity.size(); k++) {
                    GRBLinExpr expr1 = new GRBLinExpr();
                    for (int i = 0; i < data.numberJob; i++) {
                        expr1.addTerm(data.jobResource.get(i).get(k), jobActiveAtEventVars[i][e]);
                    }
                    model.addConstr(expr1, GRB.LESS_EQUAL, data.resourceCapacity.get(k),
                        "resource_" + k + "_event_" + e + " (50)");
                }
            }

            // (51) Constraints (51) and (52) set the start time of any activity i between its earliest start 
            // time ESi and its latest start time LSi.
            for (int jobIndex = 0; jobIndex < data.numberJob; jobIndex++) {
                for (int eventIndex = 0; eventIndex < data.numberJob; eventIndex++) {
                    GRBLinExpr leftSide = new GRBLinExpr();
                    GRBLinExpr middleSide = new GRBLinExpr();
                    GRBLinExpr rightSide = new GRBLinExpr();

                    leftSide.addTerm(earliestStartTime[jobIndex], jobActiveAtEventVars[jobIndex][eventIndex]);

                    middleSide.addTerm(1, startOfEventVars[eventIndex]);

                    rightSide.addTerm(latestStartTime[jobIndex], jobActiveAtEventVars[jobIndex][eventIndex]);

                    rightSide.addConstant(latestStartTime[data.numberJob - 1]);

                    rightSide.addTerm(- latestStartTime[data.numberJob - 1], jobActiveAtEventVars[jobIndex][eventIndex]);

                    if (eventIndex > 0) {
                        rightSide.addTerm(- latestStartTime[jobIndex], jobActiveAtEventVars[jobIndex][eventIndex - 1]);
                        rightSide.addTerm(latestStartTime[data.numberJob - 1], jobActiveAtEventVars[jobIndex][eventIndex - 1]);
                    }

                    model.addConstr(leftSide, GRB.LESS_EQUAL, middleSide,
                        "earliest_start_time_job_" + jobIndex + "_event_" + eventIndex + "_(51)");
                    model.addConstr(middleSide, GRB.LESS_EQUAL, rightSide,
                        "latest_start_time_job_" + jobIndex + "_event_" + eventIndex + "_(51)");
                }
            }

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
