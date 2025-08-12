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
import models.IntervalEventBasedModel.IntervalEventBasedModelSolution;

import java.util.*;

public class BuildIntervalEventSolution implements CompletionMethodInterface {

    public ModelSolutionInterface buildSolution(List<Map<Integer, Integer>> startTimesList, JobDataInstance data,
    GRBModel model) {
        int[][] earliestLatestStartTimes = DAGLongestPath.generateEarliestAndLatestStartTimes
                        (data.jobPredecessors, data.jobDuration, data.horizon);

        JobDataInstance noDummyData = DeleteDummyJobs.deleteDummyJobs(data);

        GRBVar[] startOfEventVars = new GRBVar[noDummyData.numberJob + 1];
        GRBVar[][][] jobActiveAtIntervalVars = new GRBVar[noDummyData.numberJob][startOfEventVars.length][startOfEventVars.length];

        try {
            // add startOfEvent variables
            for (int e = 0; e < startOfEventVars.length; e++) {
                startOfEventVars[e] = model.addVar(0.0, noDummyData.horizon, 0.0, GRB.CONTINUOUS, "startOfEvent[" + e + "]");
            }
            model.update(); // Ensure the model is updated after adding variables

            // add zief variables
            for (int i = 0; i < noDummyData.numberJob; i++) {
                for (int e = 0; e < startOfEventVars.length - 1; e++) {
                    for (int f = e + 1; f < startOfEventVars.length; f++) {
                        jobActiveAtIntervalVars[i][e][f] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "jobActiveAtIntervalVars[" +
                            i + "][" + e + "][" + f + "]");
                    }
                }
            }



            model.update(); // Ensure the model is updated after adding variables
            if (!startTimesList.isEmpty()) {
                Map<Integer, Integer> startTimes = null;
                model.set(GRB.IntAttr.NumStart, startTimesList.size());
                model.update();

                // Iterate through the list of start times and set the start values for the variables
                // This allows for multiple MIP starts, where each start time configuration can be tried
                // by Gurobi to find a feasible solution faster.
                // If there are multiple start times, Gurobi will try each one in sequence and choose the best one.
                
                for (int s = 0; s < model.get(GRB.IntAttr.NumStart); s++) {
                    model.set(GRB.IntParam.StartNumber, s);
                    System.out.println("Setting MIP start for index: " + s);
                    startTimes = startTimesList.get(s);

                    Map<Integer, Integer> noDummyStartTimes = DeleteDummyJobs.deleteDummyJobsFromStartTimesMap(startTimes);

                    // print start times for debugging
                    System.out.println("Start times from heuristic: " + noDummyStartTimes);

                    // Sort job indices by their start times (smallest first)
                    List<Integer> sortedJobIndices = new ArrayList<>(noDummyStartTimes.keySet());
                    sortedJobIndices.sort((job1, job2) -> Integer.compare(noDummyStartTimes.get(job1), noDummyStartTimes.get(job2)));
                    System.out.println("Sorted job indices by start time: " + sortedJobIndices);

                    // set the start values for the startOfEventVars
                    for (int eventIndex = 0; eventIndex < startOfEventVars.length - 1; eventIndex++) {
                        GRBVar var = model.getVarByName("startOfEvent[" + eventIndex + "]");
                        // event E starts at startTimes.get(eventIndex)
                        var.set(GRB.DoubleAttr.Start, noDummyStartTimes.get(sortedJobIndices.get(eventIndex)));
                        System.out.println("Setting startOfEvent[" + eventIndex + "] to " + noDummyStartTimes.get(sortedJobIndices.get(eventIndex)));
                    }

                    int heuristicMakespan = 0;
                    for (int jobIndex : noDummyStartTimes.keySet()) {
                        heuristicMakespan = Math.max(heuristicMakespan, noDummyStartTimes.get(jobIndex) + noDummyData.jobDuration.get(jobIndex));
                    }
                    GRBVar makespanVar = model.getVarByName("startOfEvent[" + (startOfEventVars.length - 1) + "]");
                    makespanVar.set(GRB.DoubleAttr.Start, heuristicMakespan);

                    List<Integer> eventGotJob = new ArrayList<>();
                    
                    model.update();
                    
                    for (int jobIndex = 0; jobIndex < noDummyData.numberJob; jobIndex++) {
                        boolean firstMatchFound = false;
                        for (int e = 0; e < startOfEventVars.length - 1; e++) {
                            for (int f = e + 1; f < startOfEventVars.length; f++) {
                                if (!firstMatchFound) {
                                    if (noDummyStartTimes.get(jobIndex) == startOfEventVars[e].get(GRB.DoubleAttr.Start)) {
                                        if (!eventGotJob.contains(e)) {
                                            if (noDummyStartTimes.get(jobIndex) + noDummyData.jobDuration.get(jobIndex) <= startOfEventVars[f].get(GRB.DoubleAttr.Start)) {
                                                jobActiveAtIntervalVars[jobIndex][e][f].set(GRB.DoubleAttr.Start, 1);
                                                firstMatchFound = true;
                                                eventGotJob.add(e);
                                                continue;
                                            }
                                        }
                                    }
                                }
                                jobActiveAtIntervalVars[jobIndex][e][f].set(GRB.DoubleAttr.Start, 0);
                            }
                        }
                    }
                }
                model.update(); // Ensure the model is updated after modifying variables
            }
        } catch (Exception e) {
            System.err.println("Error while creating a start solution from given start times: " 
                + e.getMessage());
            return null;
        }

        IntervalEventBasedModel intervalEventBasedModel = new IntervalEventBasedModel();
        IntervalEventBasedModelSolution solution = intervalEventBasedModel.new
                                        IntervalEventBasedModelSolution(startOfEventVars, jobActiveAtIntervalVars, model,
                                        earliestLatestStartTimes);

        return solution;
    }
}