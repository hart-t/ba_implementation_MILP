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
import modelSolutions.IntervalEventBasedModelSolution;

import java.util.*;

public class BuildIntervalEventSolution implements CompletionMethodInterface {

    public ModelSolutionInterface buildSolution(List<Map<Integer, Integer>> startTimesList, JobDataInstance data,
    GRBModel model, enums.WarmstartStrategy strategy) {

        long timeToCreateVariablesStart = System.nanoTime();
        long timeToCreateVariables = 0;
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

                    

                    // Sort job indices by their start times (smallest first)
                    List<Integer> sortedJobIndices = new ArrayList<>(noDummyStartTimes.keySet());
                    sortedJobIndices.sort((job1, job2) -> Integer.compare(noDummyStartTimes.get(job1), noDummyStartTimes.get(job2)));
                    System.out.println("Sorted job indices by start time: " + sortedJobIndices);

                    // set the start values for the startOfEventVars
                    for (int eventIndex = 0; eventIndex < startOfEventVars.length - 1; eventIndex++) {
                        GRBVar var = model.getVarByName("startOfEvent[" + eventIndex + "]");
                        // event E starts at startTimes.get(eventIndex)
                        if (strategy == enums.WarmstartStrategy.VS) {
                            var.set(GRB.DoubleAttr.Start, noDummyStartTimes.get(sortedJobIndices.get(eventIndex)));
                        } else if (strategy == enums.WarmstartStrategy.VH) {
                            var.set(GRB.DoubleAttr.VarHintVal, noDummyStartTimes.get(sortedJobIndices.get(eventIndex)));
                        }
                        System.out.println("Setting startOfEvent[" + eventIndex + "] to " + noDummyStartTimes.get(sortedJobIndices.get(eventIndex)));
                    }

                    int heuristicMakespan = 0;
                    for (int jobIndex : noDummyStartTimes.keySet()) {
                        heuristicMakespan = Math.max(heuristicMakespan, noDummyStartTimes.get(jobIndex) + noDummyData.jobDuration.get(jobIndex));
                    }
                    GRBVar makespanVar = model.getVarByName("startOfEvent[" + (startOfEventVars.length - 1) + "]");
                    if (strategy == enums.WarmstartStrategy.VS) {
                        makespanVar.set(GRB.DoubleAttr.Start, heuristicMakespan);
                    } else if (strategy == enums.WarmstartStrategy.VH) {
                        makespanVar.set(GRB.DoubleAttr.VarHintVal, heuristicMakespan);
                    }

                    List<Integer> eventGotJob = new ArrayList<>();
                    
                    model.update();
                    
                    for (int jobIndex = 0; jobIndex < noDummyData.numberJob; jobIndex++) {
                        boolean firstMatchFound = false;
                        for (int e = 0; e < startOfEventVars.length - 1; e++) {
                            for (int f = e + 1; f < startOfEventVars.length; f++) {
                                if (!firstMatchFound) {
                                    // Direkter Zugriff auf den Start-Zeitpunkt aus der Map
                                    int eventEStartTime = noDummyStartTimes.get(sortedJobIndices.get(e));
                                    int eventFStartTime = noDummyStartTimes.get(sortedJobIndices.get(f));
                                    
                                    if (noDummyStartTimes.get(jobIndex) == eventEStartTime) {
                                        if (!eventGotJob.contains(e)) {
                                            if (noDummyStartTimes.get(jobIndex) + noDummyData.jobDuration.get(jobIndex) <= eventFStartTime) {
                                                System.out.println("Setting jobActiveAtIntervalVars[" + jobIndex + "][" + e + "][" + f + "] to 1");
                                                if (strategy == WarmstartStrategy.VS) {
                                                    jobActiveAtIntervalVars[jobIndex][e][f].set(GRB.DoubleAttr.Start, 1);
                                                } else if (strategy == WarmstartStrategy.VH) {
                                                    jobActiveAtIntervalVars[jobIndex][e][f].set(GRB.DoubleAttr.VarHintVal, 1);
                                                }
                                                firstMatchFound = true;
                                                eventGotJob.add(e);
                                                continue;
                                            }
                                        }
                                    }
                                }
                                if (strategy == WarmstartStrategy.VS) {
                                    jobActiveAtIntervalVars[jobIndex][e][f].set(GRB.DoubleAttr.Start, 0);
                                } else if (strategy == WarmstartStrategy.VH) {
                                    jobActiveAtIntervalVars[jobIndex][e][f].set(GRB.DoubleAttr.VarHintVal, 0);
                                }
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

        IntervalEventBasedModelSolution solution = new IntervalEventBasedModelSolution(startOfEventVars, jobActiveAtIntervalVars, model,
                earliestLatestStartTimes, timeToCreateVariables);

        return solution;
    }
}