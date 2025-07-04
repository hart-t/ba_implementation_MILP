package solutionBuilder;

import com.gurobi.gurobi.GRB;
import com.gurobi.gurobi.GRBModel;
import com.gurobi.gurobi.GRBVar;

import interfaces.CompletionMethodInterface;
import interfaces.ModelSolutionInterface;
import io.JobDataInstance;
import utility.DAGLongestPath;
import models.OnOffEventBasedModel;
import models.OnOffEventBasedModel.OnOffEventBasedModelSolution;

import java.util.*;

public class BuildOnOffEventSolution implements CompletionMethodInterface {

    public ModelSolutionInterface buildSolution(Map<Integer, Integer> startTimes, JobDataInstance data, 
    GRBModel model) {

        GRBVar[][] jobActiveAtEventVars = new GRBVar[data.numberJob][data.numberJob];
        GRBVar[] startOfEventEVars = new GRBVar[data.numberJob];

        int[][] earliestLatestStartTimes = DAGLongestPath.generateEarliestAndLatestStartTimes
                (data.jobPredecessors, data.jobDuration, data.horizon);

        try {
            // add zie
            for (int i = 0; i < data.numberJob; i++) {
                for (int e = 0; e < data.numberJob; e++) {
                    jobActiveAtEventVars[i][e] = model.addVar(0.0, 1.0, 0.0, GRB.CONTINUOUS, "jobActiveAtEvent[" +
                        i + "][" + e + "]");
                }
            }

            // represents the date of the start of the event e (te)
            for (int e = 0; e < startOfEventEVars.length; e++) {
                startOfEventEVars[e] = model.addVar(0.0, data.horizon, 0.0, GRB.CONTINUOUS, "startOfEvent[" + e + "]");
            }

            model.update(); // Ensure the model is updated after adding variables
            if (!startTimes.isEmpty()) {
                // Set the start values for the starting time variables based on the provided startTimes map
                // Sort start times to assign events in chronological order
                List<Map.Entry<Integer, Integer>> sortedEntries = new ArrayList<>(startTimes.entrySet());
                sortedEntries.sort(Map.Entry.comparingByValue());
                
                int eventIndex = 0;
                for (Map.Entry<Integer, Integer> entry : sortedEntries) {
                    int job = entry.getKey();
                    int startTime = entry.getValue();
                    
                    // Set the event start time TODO test if have to do the double thing
                    // if yes i also have to alter the other solution builder
                    GRBVar var1 = model.getVarByName("startOfEvent[" + eventIndex + "]");
                    startOfEventEVars[eventIndex].set(GRB.DoubleAttr.Start, startTime);
                    var1.set(GRB.DoubleAttr.Start, startTime);
                    
                    // Set jobActiveAtEvent[job][event] to 1 for the starting event
                    GRBVar var2 = model.getVarByName("jobActiveAtEvent[" + job + "][" + eventIndex + "]");
                    jobActiveAtEventVars[job][eventIndex].set(GRB.DoubleAttr.Start, 1.0);
                    var2.set(GRB.DoubleAttr.Start, 1.0);
                    
                    // Set jobActiveAtEvent[job][event] to 1 for subsequent events while job is active
                    int jobDuration = data.jobDuration.get(job);
                    int jobEndTime = startTime + jobDuration;
                    
                    // Check all subsequent events to see if job is still active
                    for (int e = eventIndex + 1; e < data.numberJob && e < sortedEntries.size(); e++) {
                        int nextEventTime = sortedEntries.get(e).getValue();
                        // If the job is still active at this event, set variable to 1
                        if (nextEventTime < jobEndTime) {
                            GRBVar var3 = model.getVarByName("jobActiveAtEvent[" + job + "][" + e + "]");
                            jobActiveAtEventVars[job][e].set(GRB.DoubleAttr.Start, 1.0);
                            var3.set(GRB.DoubleAttr.Start, 1.0);
                        } else {
                            // Job has ended, no need to check further events
                            break;
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
                                        OnOffEventBasedModelSolution(startOfEventEVars, jobActiveAtEventVars, model,
                                         earliestLatestStartTimes);

        return solution;
    }
}