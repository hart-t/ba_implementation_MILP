package logic;

import com.gurobi.gurobi.GRB;
import com.gurobi.gurobi.GRBEnv;
import com.gurobi.gurobi.GRBModel;
import com.gurobi.gurobi.GRBVar;

import interfaces.CompletionMethodInterface;
import interfaces.ModelInterface;
import interfaces.ModelSolutionInterface;
import io.JobDataInstance;
import io.OptimizedSolution;
import io.Result;
import io.ScheduleResult;
import io.SolverResults;

public class WarmstartSolver {

    private ModelInterface model;
    private CompletionMethodInterface completionMethod;
    
    public WarmstartSolver(ModelInterface model) {
        this.model = model;
    }

    public Result solve(JobDataInstance data, ScheduleResult scheduleResult) {
        try {
            String logFile = "linear_model.log";
            completionMethod = model.getCompletionMethod();
            // Initialize the Gurobi environment and model
            GRBEnv env = new GRBEnv();
            GRBModel grbOptimizationModel = new GRBModel(env);

            // Build the solution using the completion method
            ModelSolutionInterface initialSolution = completionMethod.buildSolution(scheduleResult.getStartTimes(), data, grbOptimizationModel);

            // print the start times used to build the initial solution
            System.out.println("Initial start times used to build the solution: " + scheduleResult.getStartTimes());

            System.out.println(scheduleResult.getUsedHeuristics() + " used to build the initial solution.");
            // Complete the model with the initial solution
            grbOptimizationModel = model.completeModel(initialSolution, data);

            // Configure Gurobi parameters and logging
            grbOptimizationModel.set(GRB.DoubleParam.MIPGap, 0.0);        // Require optimal solution
            grbOptimizationModel.set(GRB.DoubleParam.TimeLimit, 10.0);    // Set time limit
            grbOptimizationModel.set(GRB.IntParam.Threads, 4);            // Use multiple threads
            grbOptimizationModel.set(GRB.IntParam.Method, 2);             // Use barrier method
            grbOptimizationModel.set(GRB.IntParam.OutputFlag, 1);         // Enable output
            grbOptimizationModel.set(GRB.StringParam.LogFile, logFile);   // Write log to file
        
            // Optimize and check solution quality
            grbOptimizationModel.optimize();

            // Print all flow variables
            // printFlowVariables(grbOptimizationModel, data);

            grbOptimizationModel.write("logFile.lp"); // Write the model to a file for debugging
            int[][] startAndFinishTimes = model.getStartAndFinishTimes(grbOptimizationModel, data);

            SolverResults solverResults = buildSolverResults(grbOptimizationModel);
            Result result = new Result(new OptimizedSolution(grbOptimizationModel, initialSolution.getModelType()), solverResults, startAndFinishTimes, scheduleResult, data.instanceName);

            // Clean up Gurobi model and environment
            grbOptimizationModel.dispose();
            env.dispose();

            return result;
    
        } catch (Exception e) {
            System.err.println("Error during solving process: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public SolverResults buildSolverResults(GRBModel model) {
        double lowerBound = 0;
        double upperBound = 0;
        double objectiveValue = 0;
        double timeInSeconds = 0;

        try {
            // upper lower bound
            lowerBound = model.get(GRB.DoubleAttr.ObjBound);
            upperBound = model.get(GRB.DoubleAttr.ObjVal);
            objectiveValue = model.get(GRB.DoubleAttr.ObjVal);
            timeInSeconds = model.get(GRB.DoubleAttr.Runtime);
        } catch (Exception e) {
            System.err.println("Error while retrieving solver results: " + e.getMessage());
            e.printStackTrace();
        }
        return new SolverResults(lowerBound, upperBound, objectiveValue, timeInSeconds);
    }

    private void printFlowVariables(GRBModel model, JobDataInstance data) {
        try {
            System.out.println("\n=== FLOW VARIABLES (Resource 1 only) ===");
            
            // Get all variables from the model
            GRBVar[] allVars = model.getVars();
            
            for (GRBVar var : allVars) {
                String varName = var.get(GRB.StringAttr.VarName);
                
                // Check if this is a flow variable for resource 1 specifically
                if (varName.contains("quantity of resource 1") && varName.contains("transferred from")) {
                    double value = var.get(GRB.DoubleAttr.X);
                    
                    // Only print non-zero flow variables for clarity
                    if (Math.abs(value) > 1e-6) {
                        System.out.printf("%-80s = %.6f%n", varName, value);
                    }
                }
            }
            
            System.out.println("=== END FLOW VARIABLES ===\n");
            
        } catch (Exception e) {
            System.err.println("Error while printing flow variables: " + e.getMessage());
            e.printStackTrace();
        }
    }
}