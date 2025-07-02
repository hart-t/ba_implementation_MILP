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

        DiscreteTimeModel discreteTimeModel = new DiscreteTimeModel();
        DiscreteTimeModelSolution solution = discreteTimeModel.new DiscreteTimeModelSolution(
            new GRBVar[data.numberJob][data.horizon], model, DAGLongestPath.generateEarliestAndLatestStartTimes
                (data.jobPredecessors, data.jobDuration, data.horizon));


        int[] earliestStartTime = solution.earliestLatestStartTimes[0];
        int[] latestStartTime = solution.earliestLatestStartTimes[1];

        // TODO muss solution.model oder reicht model?
        try {
            for (int i = 0; i < data.numberJob; i++) {
                for (int t = 0; t < data.horizon; t++) {
                    if (t >= earliestStartTime[i] && t <= latestStartTime[i]) {
                        solution.startingTimeVars[i][t] = solution.model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "startingTime[" +
                                i + "] at [" + t + "]");
                    } else {
                        solution.startingTimeVars[i][t] = null; // replaces (8)? = 0?
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error while creating starting time variables: " + e.getMessage());
            return null;
        }
        return solution;
    }
}
