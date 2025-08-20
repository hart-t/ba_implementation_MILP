package solutionBuilder;

import com.gurobi.gurobi.GRB;
import com.gurobi.gurobi.GRBModel;
import com.gurobi.gurobi.GRBVar;

import interfaces.CompletionMethodInterface;
import interfaces.ModelSolutionInterface;
import io.JobDataInstance;
import modelSolutions.DiscreteTimeModelSolution;
import utility.DAGLongestPath;

import java.util.*;

public class BuildTimeDiscreteSolution implements CompletionMethodInterface {

    public ModelSolutionInterface buildSolution(List<Map<Integer, Integer>> startTimesList, JobDataInstance data,
    GRBModel model) {

        long timeToCreateVariablesStart = System.nanoTime();
        long timeToCreateVariables = 0;
        GRBVar[][] startingTimeVars = new GRBVar[data.numberJob][data.horizon];

        int[][] earliestLatestStartTimes = DAGLongestPath.generateEarliestAndLatestStartTimes
                (data.jobPredecessors, data.jobDuration, data.horizon);
        int[] earliestStartTime = earliestLatestStartTimes[0];
        int[] latestStartTime = earliestLatestStartTimes[1];

        try {
            //adding Variables for starting times
            for (int i = 0; i < data.numberJob; i++) {
                for (int t = 0; t < data.horizon; t++) {
                    if (t >= earliestStartTime[i] && t <= latestStartTime[i]) {
                        startingTimeVars[i][t] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "startingTime[" +
                                i + "]_at_[" + t + "]");
                    } else {
                        startingTimeVars[i][t] = null;
                    }
                }
            }
            
            model.update(); // Ensure the model is updated after adding variables
            timeToCreateVariables = (System.nanoTime() - timeToCreateVariablesStart) / 1_000_000; // Convert to milliseconds

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
                        model.update();
                        System.out.println("Setting MIP start for index: " + s);
                        startTimes = startTimesList.get(s);

                        for (int i = 0; i < data.numberJob; i++) {
                            for (int j = earliestStartTime[i]; j < latestStartTime[i]; j++) {
                                GRBVar var = model.getVarByName("startingTime[" + i + "]_at_[" + j + "]");
                                if (var != null) {
                                    if (startTimes.get(i).equals(j)) {
                                        System.out.println("Setting variable for job " + i + " at time " + j + " to 1.0");
                                        var.set(GRB.DoubleAttr.Start, 1.0);
                                    } else {
                                        var.set(GRB.DoubleAttr.Start, 0.0);
                                    }
                                } else {
                                    System.err.println("Variable for job " + i + " at time " + j + " not found.");
                                }   
                            }  
                        }
                        model.update(); // Update the model after setting starts
                    }
                }
                model.update(); // Ensure the model is updated after modifying variables
        } catch (Exception e) {
            System.err.println("Error while creating a start solution from given start times: " + e.getMessage());
            return null;
        }

        DiscreteTimeModelSolution solution = new DiscreteTimeModelSolution(startingTimeVars,
                                                    model, earliestLatestStartTimes, timeToCreateVariables);
        return solution;
    }
}
