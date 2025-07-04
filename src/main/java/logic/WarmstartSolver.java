package logic;

import java.util.Map;

import com.gurobi.gurobi.GRB;
import com.gurobi.gurobi.GRBEnv;
import com.gurobi.gurobi.GRBModel;

import interfaces.CompletionMethodInterface;
import interfaces.ModelSolutionInterface;
import io.JobDataInstance;
import io.Result;

public class WarmstartSolver {
    
    private CompletionMethodInterface completionMethod;
    private ModelSolutionInterface model;
    
    public WarmstartSolver(CompletionMethodInterface completionMethod, ModelSolutionInterface model) {
        this.completionMethod = completionMethod;
        this.model = model;
    }

    public Result solve(Map<Integer, Integer> startTimes, JobDataInstance data) {
        try {
            // Initialize the Gurobi environment and model
            GRBEnv env = new GRBEnv();
            GRBModel model = new GRBModel(env);

            // Build the solution using the completion method
            ModelSolutionInterface initialSolution = completionMethod.buildSolution(startTimes, data, model);

            // 
            

            // Write model to file and optimize
            model.set(GRB.DoubleParam.MIPGap, 0.0);        // Require optimal solution
            model.set(GRB.DoubleParam.TimeLimit, 300.0);   // 5 minutes time limit
            model.set(GRB.IntParam.Threads, 4);            // Use multiple threads
            model.set(GRB.IntParam.Method, 2);             // Use barrier method
            model.set(GRB.IntParam.OutputFlag, 1);         // Enable output

            // Update model if heuristic solution computed, write to file
            // model.update();
            model.write("linear_model.lp");
        
            // Optimize and check solution quality
            model.optimize();

            Result result = null;//new Result(solution);
                
            // Clean up Gurobi model and environment
            model.dispose();
            env.dispose();

            return result;
    
        } catch (Exception e) {
            System.err.println("Error during solving process: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
}
