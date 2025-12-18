package solutionBuilder;

import com.gurobi.gurobi.GRB;
import com.gurobi.gurobi.GRBModel;
import com.gurobi.gurobi.GRBVar;

import enums.WarmstartStrategy;
import interfaces.CompletionMethodInterface;
import interfaces.ModelSolutionInterface;
import io.JobDataInstance;
import utility.DAGLongestPath;
import utility.DeleteDummyJobs;
import modelSolutions.OnOffEventBasedModelSolution;

import java.util.*;

public class BuildOnOffEventSolution implements CompletionMethodInterface {

    public ModelSolutionInterface buildSolution(List<Map<Integer, Integer>> startTimesList, JobDataInstance data,
    GRBModel model, enums.WarmstartStrategy strategy) {

        long timeToCreateVariablesStart = System.nanoTime();
        long timeToCreateVariables = 0;
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
            timeToCreateVariables = (System.nanoTime() - timeToCreateVariablesStart) / 1_000_000; // Convert to milliseconds

            if (!startTimesList.isEmpty()) {
                Map<Integer, Integer> startTimes = null;
                // model.set(GRB.IntAttr.NumStart, startTimesList.size());
                model.update();

                // Iterate through the list of start times and set the start values for the variables
                // This allows for multiple MIP starts, where each start time configuration can be tried
                // by Gurobi to find a feasible solution faster.
                // If there are multiple start times, Gurobi will try each one in sequence and choose the best one.
                
                // for (int s = 0; s < model.get(GRB.IntAttr.NumStart); s++) {
                    // model.set(GRB.IntParam.StartNumber, s);
                    // System.out.println("Setting MIP start for index: " + s);
                    startTimes = startTimesList.get(0);

                    Map<Integer, Integer> noDummyStartTimes = DeleteDummyJobs.deleteDummyJobsFromStartTimesMap(startTimes);

                    // print start times for debugging
                    System.out.println("Start times from heuristic: " + noDummyStartTimes);
                    
                    // Set the start values for the starting time variables based on the provided startTimes map
                    // Sort start times to assign events in chronological order
                    List<Map.Entry<Integer, Integer>> sortedEntries = new ArrayList<>(noDummyStartTimes.entrySet());
                    sortedEntries.sort(Map.Entry.comparingByValue());
                    System.out.println("Sorted start times: " + sortedEntries);

                    // Event counter starting at 0
                    int event = 0;
                    
                    for (Map.Entry<Integer, Integer> entry : sortedEntries) {
                        int job = entry.getKey();
                        int startTime = entry.getValue();
                        
                        // Job is set to be active at one (the corresponding) event
                        if (strategy == WarmstartStrategy.VS) {
                            jobActiveAtEventVars[job][event].set(GRB.DoubleAttr.Start, 1.0);
                        } else if (strategy == WarmstartStrategy.VH) {
                            jobActiveAtEventVars[job][event].set(GRB.DoubleAttr.VarHintVal, 1.0);
                        }

                        // The start time of the event is set to the start time of the job given by the heuristic
                        if (strategy == WarmstartStrategy.VS) {
                            startOfEventEVars[event].set(GRB.DoubleAttr.Start, startTime);
                        } else if (strategy == WarmstartStrategy.VH) {
                            startOfEventEVars[event].set(GRB.DoubleAttr.VarHintVal, startTime);
                        }
                        
                        event++;
                    }

                    model.update(); // Ensure the model is updated after setting start values

                    // For each job, check all events and set jobActiveAtEventVars accordingly
                    for (Map.Entry<Integer, Integer> jobEntry : noDummyStartTimes.entrySet()) {
                        int job = jobEntry.getKey();
                        int jobStartTime = jobEntry.getValue();
                        int jobEndTime = jobStartTime + noDummyData.jobDuration.get(job);
                        
                        System.out.println("Processing job " + job + ": start=" + jobStartTime + ", end=" + jobEndTime);
                        
                        // Check against all events using the stored start times
                        for (int e = 0; e < noDummyData.numberJob; e++) {
                            try {
                                // Get event start time from sorted entries
                                int eventStartTime = (e < sortedEntries.size()) ? sortedEntries.get(e).getValue() : 0;
                                
                                if (eventStartTime < jobStartTime) {
                                    // Event starts before job starts -> job not active
                                    if (strategy == WarmstartStrategy.VS) {
                                        jobActiveAtEventVars[job][e].set(GRB.DoubleAttr.Start, 0.0);
                                    } else if (strategy == WarmstartStrategy.VH) {
                                        jobActiveAtEventVars[job][e].set(GRB.DoubleAttr.VarHintVal, 0.0);
                                    }
                                } else if (eventStartTime >= jobStartTime && eventStartTime < jobEndTime) {
                                    // Event starts during job execution -> job active
                                    if (strategy == WarmstartStrategy.VS) {
                                        jobActiveAtEventVars[job][e].set(GRB.DoubleAttr.Start, 1.0);
                                    } else if (strategy == WarmstartStrategy.VH) {
                                        jobActiveAtEventVars[job][e].set(GRB.DoubleAttr.VarHintVal, 1.0);
                                    }
                                } else if (eventStartTime >= jobEndTime) {
                                    // Event starts after job ends -> job not active
                                    if (strategy == WarmstartStrategy.VS) {
                                        jobActiveAtEventVars[job][e].set(GRB.DoubleAttr.Start, 0.0);
                                    } else if (strategy == WarmstartStrategy.VH) {
                                        jobActiveAtEventVars[job][e].set(GRB.DoubleAttr.VarHintVal, 0.0);
                                    }
                                }
                            } catch (Exception ex) {
                                System.err.println("Error processing job " + job + " event " + e + ": " + ex.getMessage());
                            }
                        }
                    }
                // }
                model.update(); // Ensure the model is updated after modifying variables
            }        
        } catch (Exception e) {
            System.err.println("Error while creating a start solution from given start times: " 
                + e.getMessage());
            return null;
        }

        OnOffEventBasedModelSolution solution = new OnOffEventBasedModelSolution(makespanVar, startOfEventEVars, jobActiveAtEventVars, model,
                                        earliestLatestStartTimes, timeToCreateVariables);

        return solution;
    }
}