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

    public ModelSolutionInterface buildSolution(Map<Integer, Integer> startTimes, JobDataInstance data, 
    GRBModel model) {
        int[][] earliestLatestStartTimes = DAGLongestPath.generateEarliestAndLatestStartTimes
                        (data.jobPredecessors, data.jobDuration, data.horizon);
        earliestLatestStartTimes = DeleteDummyJobs.deleteDummyJobsFromEarliestLatestStartTimes(earliestLatestStartTimes);

        GRBVar[] startOfEventVars = new GRBVar[data.numberJob];
        GRBVar[][][] ziefVars = new GRBVar[data.numberJob][startOfEventVars.length][startOfEventVars.length];

        try {
            // add startOfEvent variables
            for (int e = 0; e < startOfEventVars.length; e++) {
                startOfEventVars[e] = model.addVar(0.0, data.horizon, 0.0, GRB.CONTINUOUS, "startOfEvent[" + e + "]");
            }

            // add zief variables
            for (int i = 0; i < data.numberJob; i++) {
                for (int e = 0; e < startOfEventVars.length; e++) {
                    for (int f = 0; f < startOfEventVars.length; f++) {
                        ziefVars[i][e][f] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "zief[" +
                            i + "][" + e + "][" + f + "]");
                    }
                }
            }



            model.update(); // Ensure the model is updated after adding variables
            if (!startTimes.isEmpty()) {
                
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
                    //jobActiveAtEventVars[job][event].set(GRB.DoubleAttr.Start, 1.0);
                    var1.set(GRB.DoubleAttr.Start, 1.0);

                    // The start time of the event is set to the start time of the job given by the heuristic
                    GRBVar var2 = model.getVarByName("startOfEvent[" + event + "]");
                    //startOfEventEVars[event].set(GRB.DoubleAttr.Start, startTime);
                    var2.set(GRB.DoubleAttr.Start, startTime);
                    
                    event++;
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
                                        IntervalEventBasedModelSolution(startOfEventVars, ziefVars, model,
                                         earliestLatestStartTimes);

        return solution;
    }
}