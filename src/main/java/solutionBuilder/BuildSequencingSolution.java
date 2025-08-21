package solutionBuilder;

import java.util.List;
import java.util.Map;

import com.gurobi.gurobi.GRB;
import com.gurobi.gurobi.GRBModel;
import com.gurobi.gurobi.GRBVar;

import interfaces.CompletionMethodInterface;
import interfaces.ModelSolutionInterface;
import io.JobDataInstance;
import modelSolutions.FlowBasedContinuousTimeModelSolution;
import utility.DAGLongestPath;
import utility.TEMatrix;

public class BuildSequencingSolution implements CompletionMethodInterface{
    public ModelSolutionInterface buildSolution(List<Map<Integer, Integer>> startTimesList, JobDataInstance data,
    GRBModel model) {

        long timeToCreateVariablesStart = System.nanoTime();
        long timeToCreateVariables = 0;

        GRBVar[] si = new GRBVar[data.numberJob];
        GRBVar[][] yij = new GRBVar[data.numberJob][data.numberJob];
        GRBVar[][] zij = new GRBVar[data.numberJob][data.numberJob];

        int[][] earliestLatestStartTimes = DAGLongestPath.generateEarliestAndLatestStartTimes
                (data.jobPredecessors, data.jobDuration, data.horizon);

        // generate TE matrix, This means that if there is a path from activity i to j in the precedence graph
        // (even if not directly), then (i,j) element TE.
        int[][] teMatrix = TEMatrix.computeTEMatrix(data.numberJob, data.jobSuccessors, data.jobDuration);

        try {
            // add starting time variables
            for (int i = 0; i < data.numberJob; i++) {
                si[i] = model.addVar(0.0, data.horizon, 0.0, GRB.CONTINUOUS, "startingTime[" +
                        i + "]");
            }

            // equals 1, if activity i is completed at any time before or at the same time as activity j starts, 0 otherwise
            for (int i = 0; i < data.numberJob; i++) {
                for (int j = 0; j < data.numberJob; j++) {
                    if (i == j) continue; // Skip self-precedence
                    yij[i][j] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "completionTime_[" + i +
                            "]_<=_startingTime_[" + j + "]");
                }
            }

            // equals 1, if activity i starts at any time before or at the same time as activity j starts, 0 otherwise
            for (int i = 0; i < data.numberJob; i++) {
                for (int j = 0; j < data.numberJob; j++) {
                    if (i == j || teMatrix[i][j] == 1 || teMatrix[j][i] == 1) continue; // Skip self-precedence, transitive edges
                    zij[i][j] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "startingTime_[" + i +
                            "]_<=_startingTime_[" + j + "]");
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
                    System.out.println("Setting MIP start for index: " + s);
                    startTimes = startTimesList.get(s);

                    // Set the start values for the starting time variables based on the provided startTimes map
                    for (int i = 0; i < data.numberJob; i++) {
                        int startTime = startTimes.get(i);
                        GRBVar var = startingTimeVars[i];
                        if (var != null) {
                            // Set the start value for the variable (job i starts at startTime)
                            var.set(GRB.DoubleAttr.Start, startTime);
                        } else {
                            System.err.println("Variable for job " + i + " at time " + startTime + " not found.");
                        }   
                    }

                    // Set the precedence variables based on the starting times
                    for (int i = 0; i < data.numberJob; i++) {
                        for (int j = i + 1; j < data.numberJob; j++) {
                            //if (i == j) continue; // Skip self-precedence
                            GRBVar var1 = precedenceVars[i][j];
                            GRBVar var2 = precedenceVars[j][i];

                            if (startTimes.get(i) + data.jobDuration.get(i) <= startTimes.get(j)) {
                                // Set the precedence value for the precedence variable
                                var1.set(GRB.DoubleAttr.Start, 1.0);
                            }
                            else if (startTimes.get(i) >= startTimes.get(j) + data.jobDuration.get(j)) {
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
                                GRBVar flowVar = continuousFlowVars[i][j][k];
                                double flowAmount = flowVars[i][j][k];
                                flowVar.set(GRB.DoubleAttr.Start, flowAmount);
                            }
                        }
                    }

                    // Set flow variables from supersink (last job) to supersource (first job) to resource 
                    // capacity for Constraint (18)
                    int supersource = 0;
                    int supersink = data.numberJob - 1;

                    for (int k = 0; k < data.resourceCapacity.size(); k++) {
                        GRBVar flowVar = model.getVarByName("quantity of resource " + k + "transferred from " + supersink + " to " + supersource);
                        double resourceCapacity = data.resourceCapacity.get(k);
                            flowVar.set(GRB.DoubleAttr.Start, resourceCapacity);
                    }
                    model.update(); // Update the model after setting starts
                }
                model.update(); // Ensure the model is updated after modifying variables
            } else {
                System.out.println("No start times provided, using default values.");
            }
            model.update(); // Ensure the model is updated after modifying variables
        } catch (Exception e) {
            System.err.println("Error while creating a start solution from given start times: " + e.getMessage());
            return null;
        }

        FlowBasedContinuousTimeModelSolution solution = new FlowBasedContinuousTimeModelSolution(startingTimeVars, precedenceVars,
                continuousFlowVars, model, earliestLatestStartTimes, timeToCreateVariables);

        return solution;
    }
}
