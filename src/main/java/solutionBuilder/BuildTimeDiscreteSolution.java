package solutionBuilder;

import com.gurobi.gurobi.GRB;
import com.gurobi.gurobi.GRBModel;
import com.gurobi.gurobi.GRBVar;

import interfaces.CompletionMethodInterface;
import interfaces.ModelSolutionInterface;
import io.JobDataInstance;
import models.DiscreteTimeModel;
import models.DiscreteTimeModel.DiscreteTimeModelSolution;
import utility.DAGLongestPath;

import java.util.*;

public class BuildTimeDiscreteSolution implements CompletionMethodInterface {

    public ModelSolutionInterface buildSolution(Map<Integer, Integer> startTimes, JobDataInstance data, 
    GRBModel model) {

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
                                i + "] at [" + t + "]");
                    } else {
                        startingTimeVars[i][t] = null; // replaces (8)? = 0?
                    }
                }
            }
            model.update(); // Ensure the model is updated after adding variables
            if (!startTimes.isEmpty()) {
                for (int i = 0; i < data.numberJob; i++) {
                    int startTime = startTimes.get(i); // Start time for job i
                    // Get the variable corresponding to job i starting at startTime
                    GRBVar var = model.getVarByName("startingTime[" + i + "] at [" + startTime + "]");
                    if (var != null) {
                        // Set the start value for the variable to 1 (job i starts at startTime)
                        var.set(GRB.DoubleAttr.Start, 1.0);
                    } else {
                        System.err.println("Variable for job " + i + " at time " + startTime + " not found.");
                    }   
                }
            }
            model.update(); // Ensure the model is updated after modifying variables
        } catch (Exception e) {
            System.err.println("Error while creating starting time variables: " + e.getMessage());
            return null;
        }

        DiscreteTimeModel discreteTimeModel = new DiscreteTimeModel();
        DiscreteTimeModelSolution solution = discreteTimeModel.new DiscreteTimeModelSolution(startingTimeVars,
                                                    model, earliestLatestStartTimes);
        return solution;
    }
}
