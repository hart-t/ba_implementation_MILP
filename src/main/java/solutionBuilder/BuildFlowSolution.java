package solutionBuilder;

import com.gurobi.gurobi.GRB;
import com.gurobi.gurobi.GRBModel;
import com.gurobi.gurobi.GRBVar;

import interfaces.CompletionMethodInterface;
import interfaces.ModelSolutionInterface;
import io.JobDataInstance;
import models.FlowBasedContinuousTimeModel;
import models.FlowBasedContinuousTimeModel.FlowBasedContinuousTimeModelSolution;
import utility.DAGLongestPath;

import java.util.*;

public class BuildFlowSolution implements CompletionMethodInterface {

    public ModelSolutionInterface buildSolution(Map<Integer, Integer> startTimes, JobDataInstance data, 
    GRBModel model) {

        GRBVar[] startingTimeVars = new GRBVar[data.numberJob];
        GRBVar[][] precedenceVars =new GRBVar[data.numberJob][data.numberJob];
        GRBVar[][][] continuousFlowVars =new GRBVar[data.numberJob][data.numberJob][data.resourceCapacity.size()];

        int[][] earliestLatestStartTimes = DAGLongestPath.generateEarliestAndLatestStartTimes
                (data.jobPredecessors, data.jobDuration, data.horizon);

        try {
            // add starting time variables
            for (int i = 0; i < data.numberJob; i++) {
                startingTimeVars[i] = model.addVar(0.0, data.horizon, 0.0, GRB.CONTINUOUS, "startingTime[" +
                        i + "]");
            }

            // indicate whether activity i is processed before activity j
            for (int i = 0; i < data.numberJob; i++) {
                for (int j = 0; j < data.numberJob; j++) {
                    if (i == j) continue; // Skip self-precedence
                    precedenceVars[i][j] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "[" + i +
                            "] precedes [" + j + "]");
                }
            }

            // Finally, continuous flow variables are introduced to denote the quantity of resource k that is transferred
            // from activity i (at the end of its processing) to activity j (at the start of its processing).
            for (int i = 0; i < data.numberJob; i++) {
                for (int j = 0; j < data.numberJob; j++) {
                    if (i == j) continue; // Skip self-transfer
                    for (int k = 0; k < data.resourceCapacity.size(); k++) {
                        continuousFlowVars[i][j][k] = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS,
                                "quantity of resource " + k + "transferred from " + i + " to " + j);
                    }
                }
            }

            model.update(); // Ensure the model is updated after adding variables
            if (!startTimes.isEmpty()) {
                // Set the start values for the starting time variables based on the provided startTimes map
                for (int i = 0; i < data.numberJob; i++) {
                    int startTime = startTimes.get(i);
                    GRBVar var = model.getVarByName("startingTime[" + i + "]");
                    if (var != null) {
                        // Set the start value for the variable (job i starts at startTime)
                        var.set(GRB.DoubleAttr.Start, startTime);
                    } else {
                        System.err.println("Variable for job " + i + " at time " + startTime + " not found.");
                    }   
                }

                // Set the precedence variables based on the starting times
                for (int i = 0; i < data.numberJob; i++) {
                    for (int j = 0; j < data.numberJob; j++) {
                        if (i == j) continue; // Skip self-precedence
                        GRBVar var1 = model.getVarByName("[" + i + "] precedes [" + j + "]");
                        GRBVar var2 = model.getVarByName("[" + j + "] precedes [" + i + "]");

                        if (startTimes.get(i) < startTimes.get(j)) {
                            // Set the precedence value for the precedence variable
                            var1.set(GRB.DoubleAttr.Start, 1.0);
                        }
                        else if (startTimes.get(i) > startTimes.get(j)) {
                            // Set the precedence value for the precedence variable
                            var2.set(GRB.DoubleAttr.Start, 1.0);
                        }
                    }
                }

                // Set flow variables using NetworkFlowProblem utility
                utility.NetworkFlowProblem networkFlow = new utility.NetworkFlowProblem();
                int[][][] flowVars = networkFlow.findFlowVariables(data, startTimes);
                
                // Set the flow variables in the Gurobi model
                for (int i = 0; i < data.numberJob; i++) {
                    for (int j = 0; j < data.numberJob; j++) {
                        if (i == j) continue; // Skip self-transfer
                        
                        for (int k = 0; k < data.resourceCapacity.size(); k++) {
                            GRBVar flowVar = model.getVarByName("quantity of resource " + k + "transferred from " + i + " to " + j);
                            if (flowVar != null) {
                                double flowAmount = flowVars[i][j][k];
                                flowVar.set(GRB.DoubleAttr.Start, flowAmount);
                            }
                        }
                    }
                }
            } else {
                System.out.println("No start times provided, using default values.");
            }
            model.update(); // Ensure the model is updated after modifying variables
        } catch (Exception e) {
            System.err.println("Error while creating a start solution from given start times: " + e.getMessage());
            return null;
        }

        FlowBasedContinuousTimeModel flowBasedContinuousTimeModel = new FlowBasedContinuousTimeModel();
        FlowBasedContinuousTimeModelSolution solution = flowBasedContinuousTimeModel.new 
                                        FlowBasedContinuousTimeModelSolution(startingTimeVars, precedenceVars,
                                         continuousFlowVars, model, earliestLatestStartTimes);

        return solution;
    }
}
