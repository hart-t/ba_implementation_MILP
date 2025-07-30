package solutionBuilder;

import com.gurobi.gurobi.GRB;
import com.gurobi.gurobi.GRBModel;
import com.gurobi.gurobi.GRBVar;

import interfaces.CompletionMethodInterface;
import interfaces.ModelSolutionInterface;
import io.JobDataInstance;
import utility.DAGLongestPath;
import utility.DeleteDummyJobs;
import models.IntervalEventBasedModel;
import models.IntervalEventBasedModel.IntervallEventBasedModelSolution;

import java.util.*;

public class BuildIntervalEventSolution implements CompletionMethodInterface {

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

                // print start times for debugging
                System.out.println("Start times from heuristic: " + startTimes);
                
                // Set the start values for the starting time variables based on the provided startTimes map
                // Sort start times to assign events in chronological order
                List<Map.Entry<Integer, Integer>> sortedEntries = new ArrayList<>(startTimes.entrySet());
                sortedEntries.sort(Map.Entry.comparingByValue());
                System.out.println("Sorted start times: " + sortedEntries);

                // Update the model
                model.update();

                // Event counter starting at 0
                int event = 0;
                
                for (Map.Entry<Integer, Integer> entry : sortedEntries) {
                    int job = entry.getKey();
                    int startTime = entry.getValue();
                    
                    // Set jobActiveAtEventVars[job][counter] to 1
                    GRBVar var1 = model.getVarByName("jobActiveAtEvent[" + job + "][" + event + "]");
                
                    // Job is set to be active at one (the corresponding) event
                    jobActiveAtEventVars[job][event].set(GRB.DoubleAttr.Start, 1.0);
                    var1.set(GRB.DoubleAttr.Start, 1.0);

                    // The start time of the event is set to the start time of the job given by the heuristic
                    GRBVar var2 = model.getVarByName("startOfEvent[" + event + "]");
                    startOfEventEVars[event].set(GRB.DoubleAttr.Start, startTime);
                    var2.set(GRB.DoubleAttr.Start, startTime);
                    
                    event++;
                }

                model.update(); // Ensure the model is updated after setting start values

                // For each job, check all events and set jobActiveAtEventVars accordingly
                for (Map.Entry<Integer, Integer> jobEntry : startTimes.entrySet()) {
                    int job = jobEntry.getKey();
                    int jobStartTime = jobEntry.getValue();
                    int jobEndTime = jobStartTime + noDummyData.jobDuration.get(job);
                    
                    System.out.println("Processing job " + job + ": start=" + jobStartTime + ", end=" + jobEndTime);
                    
                    // Check against all events
                    for (int e = 0; e < noDummyData.numberJob; e++) {
                        try {
                            // Get the start time of this event
                            double eventStartTime = startOfEventEVars[e].get(GRB.DoubleAttr.Start);
                            
                            GRBVar jobActiveVar = model.getVarByName("jobActiveAtEvent[" + job + "][" + e + "]");
                            
                            if (eventStartTime < jobStartTime) {
                                // Event starts before job starts -> job not active
                                jobActiveAtEventVars[job][e].set(GRB.DoubleAttr.Start, 0.0);
                                if (jobActiveVar != null) {
                                    jobActiveVar.set(GRB.DoubleAttr.Start, 0.0);
                                }
                            } else if (eventStartTime >= jobStartTime && eventStartTime < jobEndTime) {
                                // Event starts during job execution -> job active
                                jobActiveAtEventVars[job][e].set(GRB.DoubleAttr.Start, 1.0);
                                if (jobActiveVar != null) {
                                    jobActiveVar.set(GRB.DoubleAttr.Start, 1.0);
                                }
                            } else if (eventStartTime >= jobEndTime) {
                                // Event starts after job ends -> job not active
                                jobActiveAtEventVars[job][e].set(GRB.DoubleAttr.Start, 0.0);
                                if (jobActiveVar != null) {
                                    jobActiveVar.set(GRB.DoubleAttr.Start, 0.0);
                                }
                            }

                            
                        } catch (Exception ex) {
                            System.err.println("Error processing job " + job + " event " + e + ": " + ex.getMessage());
                        }
                    }
                }
            }
            model.update(); // Ensure the model is updated after modifying variables
        } catch (Exception e) {
            System.err.println("Error while creating a start solution from given start times: " 
                + e.getMessage());
            return null;
        }

        IntervalEventBasedModel intervalEventBasedModel = new IntervalEventBasedModel();
        IntervalEventBasedModelSolution solution = intervalEventBasedModel.new
                                        IntervalEventBasedModelSolution(makespanVar, startOfEventEVars, jobActiveAtEventVars, model,
                                         earliestLatestStartTimes);

        return solution;
    }
}