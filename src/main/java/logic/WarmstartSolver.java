package logic;

import io.FileReader;
import java.util.Map;

import com.gurobi.gurobi.GRB;
import com.gurobi.gurobi.GRBConstr;
import com.gurobi.gurobi.GRBEnv;
import com.gurobi.gurobi.GRBModel;

import interfaces.CompletionMethodInterface;
import interfaces.ModelInterface;
import interfaces.ModelSolutionInterface;
import io.JobDataInstance;
import io.OptimizedSolution;
import io.Result;
import io.SolverResults;

public class WarmstartSolver {

    private ModelInterface model;
    private CompletionMethodInterface completionMethod;
    
    public WarmstartSolver(ModelInterface model) {
        this.model = model;
    }

    public Result solve(Map<Integer, Integer> startTimes, JobDataInstance data) {
        try {
            String logFile = "linear_model.log";
            completionMethod = model.getCompletionMethod();
            // Initialize the Gurobi environment and model
            GRBEnv env = new GRBEnv();
            GRBModel grbOptimizationModel = new GRBModel(env);

            // Build the solution using the completion method
            ModelSolutionInterface initialSolution = completionMethod.buildSolution(startTimes, data, grbOptimizationModel);

            // Complete the model with the initial solution
            grbOptimizationModel = model.completeModel(initialSolution, data);

            // Configure Gurobi parameters and logging
            grbOptimizationModel.set(GRB.DoubleParam.MIPGap, 0.0);        // Require optimal solution
            grbOptimizationModel.set(GRB.DoubleParam.TimeLimit, 180.0);   // 15 seconds time limit for testing
            grbOptimizationModel.set(GRB.IntParam.Threads, 4);            // Use multiple threads
            grbOptimizationModel.set(GRB.IntParam.Method, 2);             // Use barrier method
            grbOptimizationModel.set(GRB.IntParam.OutputFlag, 1);         // Enable output
            grbOptimizationModel.set(GRB.StringParam.LogFile, logFile);   // Write log to file
        



            // Add this right before model.optimize() in your solving code
            try {
                grbOptimizationModel.optimize();
                
                if (grbOptimizationModel.get(GRB.IntAttr.Status) == GRB.INFEASIBLE) {
                    System.out.println("Model is infeasible. Computing IIS...");
                    grbOptimizationModel.computeIIS();
                    grbOptimizationModel.write("model_iis.ilp");
                    System.out.println("IIS written to model_iis.ilp");
                    
                    // Print conflicting constraints
                    GRBConstr[] constrs = grbOptimizationModel.getConstrs();
                    for (GRBConstr constr : constrs) {
                        if (constr.get(GRB.IntAttr.IISConstr) > 0) {
                            System.out.println("Conflicting constraint: " + constr.get(GRB.StringAttr.ConstrName));
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }





            // Optimize and check solution quality
            grbOptimizationModel.optimize();

            grbOptimizationModel.write("logFile.lp"); // Write the model to a file for debugging
            int[][] startAndFinishTimes = model.getStartAndFinishTimes(grbOptimizationModel, data);

            SolverResults solverResults = buildSolverResults(grbOptimizationModel);
            Result result = new Result(new OptimizedSolution(grbOptimizationModel), solverResults, startAndFinishTimes);
                
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
            lowerBound = model.get(GRB.DoubleAttr.ObjBound);
            upperBound = model.get(GRB.DoubleAttr.ObjBoundC);
            objectiveValue = model.get(GRB.DoubleAttr.ObjVal);
            timeInSeconds = model.get(GRB.DoubleAttr.Runtime);
        } catch (Exception e) {
            System.err.println("Error while retrieving solver results: " + e.getMessage());
            e.printStackTrace();
        }
        return new SolverResults(lowerBound, upperBound, objectiveValue, timeInSeconds);
    }
}