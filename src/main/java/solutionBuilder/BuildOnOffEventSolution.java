package solutionBuilder;

import com.gurobi.gurobi.GRB;
import com.gurobi.gurobi.GRBModel;
import com.gurobi.gurobi.GRBVar;

import interfaces.CompletionMethodInterface;
import interfaces.ModelSolutionInterface;
import io.JobDataInstance;
import utility.DAGLongestPath;
import utility.DeleteDummyJobs;
import models.OnOffEventBasedModel;
import models.OnOffEventBasedModel.OnOffEventBasedModelSolution;

import java.util.*;

public class BuildOnOffEventSolution implements CompletionMethodInterface {

    public ModelSolutionInterface buildSolution(Map<Integer, Integer> startTimes, JobDataInstance data, 
    GRBModel model) {
        int[][] earliestLatestStartTimes = DAGLongestPath.generateEarliestAndLatestStartTimes
                        (data.jobPredecessors, data.jobDuration, data.horizon);
        earliestLatestStartTimes = DeleteDummyJobs.deleteDummyJobsFromEarliestLatestStartTimes(earliestLatestStartTimes);
        
        JobDataInstance noDummyData = DeleteDummyJobs.deleteDummyJobs(data);

        GRBVar makespanVar;
        GRBVar[] startOfEventEVars = new GRBVar[noDummyData.numberJob];
        GRBVar[][] jobActiveAtEventVars = new GRBVar[noDummyData.numberJob][noDummyData.numberJob];
        
        try {
            makespanVar = model.addVar(0.0, noDummyData.horizon, 0.0, GRB.CONTINUOUS, "makespan");
            // add zie
            for (int i = 0; i < noDummyData.numberJob; i++) {
                for (int e = 0; e < noDummyData.numberJob; e++) {
                    jobActiveAtEventVars[i][e] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "jobActiveAtEvent[" +
                        i + "][" + e + "]");
                }
            }

            // represents the date of the start of the event e (te)
            for (int e = 0; e < startOfEventEVars.length; e++) {
                startOfEventEVars[e] = model.addVar(0.0, noDummyData.horizon, 0.0, GRB.CONTINUOUS, "startOfEvent[" + e + "]");
            }



            model.update(); // Ensure the model is updated after adding variables
            if (!startTimes.isEmpty()) {
                
                startTimes = DeleteDummyJobs.deleteDummyJobsFromStartTimesMap(startTimes);
                
                // Set the start values for the starting time variables based on the provided startTimes map
                // Sort start times to assign events in chronological order
                List<Map.Entry<Integer, Integer>> sortedEntries = new ArrayList<>(startTimes.entrySet());
                sortedEntries.sort(Map.Entry.comparingByValue());
                
                // Group jobs by their start times to assign correct event indices
                Map<Integer, List<Integer>> timeToJobs = new LinkedHashMap<>();
                for (Map.Entry<Integer, Integer> entry : sortedEntries) {
                    int startTime = entry.getValue();
                    timeToJobs.computeIfAbsent(startTime, k -> new ArrayList<>()).add(entry.getKey());
                }
                
                int eventIndex = 0;
                int makespan = 0;
                
                for (Map.Entry<Integer, List<Integer>> timeEntry : timeToJobs.entrySet()) {
                    int startTime = timeEntry.getKey();
                    List<Integer> jobsAtThisTime = timeEntry.getValue();
                    
                    // Bounds check
                    if (eventIndex >= noDummyData.numberJob) {
                        System.err.println("Warning: More events than expected. Stopping at event " + eventIndex);
                        break;
                    }
                    
                    // Set the event start time
                    GRBVar var1 = model.getVarByName("startOfEvent[" + eventIndex + "]");
                    if (var1 != null) {
                        startOfEventEVars[eventIndex].set(GRB.DoubleAttr.Start, startTime);
                        var1.set(GRB.DoubleAttr.Start, startTime);
                    }
                    
                    // Process all jobs starting at this time
                    for (int job : jobsAtThisTime) {
                        System.out.println("TEST  " + job + " " + startTime + " event " + eventIndex);
                        
                        // Bounds check for job index
                        if (job >= noDummyData.numberJob) {
                            System.err.println("Warning: Job index " + job + " exceeds bounds " + noDummyData.numberJob);
                            continue;
                        }
                        
                        // Set jobActiveAtEvent[job][event] to 1 for the starting event
                        GRBVar var2 = model.getVarByName("jobActiveAtEvent[" + job + "][" + eventIndex + "]");
                        if (var2 != null) {
                            jobActiveAtEventVars[job][eventIndex].set(GRB.DoubleAttr.Start, 1.0);
                            var2.set(GRB.DoubleAttr.Start, 1.0);
                        }
                        
                        // Set jobActiveAtEvent[job][event] to 1 for subsequent events while job is active
                        int jobDuration = noDummyData.jobDuration.get(job);
                        int jobEndTime = startTime + jobDuration;

                        if (makespan < jobEndTime) {
                            makespan = jobEndTime;
                            makespanVar.set(GRB.DoubleAttr.Start, makespan);
                        }
                        
                        // Check all subsequent events to see if job is still active
                        int nextEventIndex = eventIndex + 1;
                        for (Map.Entry<Integer, List<Integer>> futureTimeEntry : timeToJobs.entrySet()) {
                            if (futureTimeEntry.getKey() <= startTime) continue; // Skip current and past events
                            if (nextEventIndex >= noDummyData.numberJob) break; // Bounds check
                            
                            int nextEventTime = futureTimeEntry.getKey();
                            // If the job is still active at this event, set variable to 1
                            if (nextEventTime < jobEndTime) {
                                GRBVar var3 = model.getVarByName("jobActiveAtEvent[" + job + "][" + nextEventIndex + "]");
                                if (var3 != null && nextEventIndex < noDummyData.numberJob) {
                                    jobActiveAtEventVars[job][nextEventIndex].set(GRB.DoubleAttr.Start, 1.0);
                                    var3.set(GRB.DoubleAttr.Start, 1.0);
                                }
                            } else {
                                // Job has ended, no need to check further events
                                break;
                            }
                            nextEventIndex++;
                        }
                    }
                    
                    eventIndex++;
                }
            }
            model.update(); // Ensure the model is updated after modifying variables
        } catch (Exception e) {
            System.err.println("Error while creating a start solution from given start times: " 
                + e.getMessage());
            return null;
        }

        OnOffEventBasedModel onOffEventBasedModel = new OnOffEventBasedModel();
        OnOffEventBasedModelSolution solution = onOffEventBasedModel.new 
                                        OnOffEventBasedModelSolution(makespanVar, startOfEventEVars, jobActiveAtEventVars, model,
                                         earliestLatestStartTimes);


                                         //TODO delete dummy jobs from earliestLatestStartTimes
        return solution;
    }
}