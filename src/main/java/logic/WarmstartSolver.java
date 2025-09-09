package logic;

import com.gurobi.gurobi.GRB;
import com.gurobi.gurobi.GRBEnv;
import com.gurobi.gurobi.GRBModel;

import gurobi.ObjValueCallback;

import com.gurobi.gurobi.GRBConstr;

import interfaces.CompletionMethodInterface;
import interfaces.ModelInterface;
import interfaces.ModelSolutionInterface;
import io.JobDataInstance;
import io.OptimizedSolution;
import io.Result;
import io.ScheduleResult;
import io.SolverResults;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import io.CallbackValues;

public class WarmstartSolver {

    private ModelInterface model;
    private CompletionMethodInterface completionMethod;
    private int maxRuntime;
    private Map<Integer, Integer> targetFunctionValueCurve;

    public WarmstartSolver(ModelInterface model, int maxRuntime) {
        this.model = model;
        this.maxRuntime = maxRuntime;
    }

    public Result solve(JobDataInstance data, ScheduleResult scheduleResult) {
        try {
            String logFile = "linear_model.log";
            completionMethod = model.getCompletionMethod();
            System.out.println("Using completion method: " + completionMethod.getClass().getSimpleName());

            // Initialize the target function value curve map
            targetFunctionValueCurve = new HashMap<>();

            // Initialize the Gurobi environment and model
            GRBEnv env = new GRBEnv();
            GRBModel grbOptimizationModel = new GRBModel(env);

            // Build the solution using the completion method
            long startTime = System.nanoTime();
            ModelSolutionInterface initialSolution = completionMethod.buildSolution(scheduleResult.getStartTimes(), data, grbOptimizationModel);
            long timeComputingAndBuildingHeuristicStartSolution = System.nanoTime() - startTime - initialSolution.getTimeToCreateVariables() + scheduleResult.getTimeComputingHeuristicStartTimes();

            // print the start times used to build the initial solution
            System.out.println("Initial start times used to build the solution: " + scheduleResult.getStartTimes());

            System.out.println(scheduleResult.getUsedHeuristics() + " used to build the initial solution.");
            // Complete the model with the initial solution
            grbOptimizationModel = model.completeModel(initialSolution, data);

            // Configure Gurobi parameters and logging
            grbOptimizationModel.set(GRB.DoubleParam.MIPGap, 0.0);        // Require optimal solution
            grbOptimizationModel.set(GRB.DoubleParam.TimeLimit, maxRuntime);   // Set time limit
            grbOptimizationModel.set(GRB.IntParam.Threads, 4);            // Use multiple threads
            grbOptimizationModel.set(GRB.IntParam.Method, 2);             // Use barrier method
            grbOptimizationModel.set(GRB.IntParam.OutputFlag, 1);         // Enable output
            grbOptimizationModel.set(GRB.StringParam.LogFile, logFile);   // Write log to file
    
            // Callback with reference to the target function value curve
            ObjValueCallback objValueCallback = new ObjValueCallback();
            objValueCallback.setTargetFunctionValueCurve(targetFunctionValueCurve); // You'll need to add this method
            grbOptimizationModel.setCallback(objValueCallback);

            // Optimize and check solution quality
            grbOptimizationModel.optimize();

            // Print model statistics
            System.out.println("Model Statistics:");
            System.out.println("Number of variables: " + grbOptimizationModel.get(GRB.IntAttr.NumVars));
            System.out.println("Number of constraints: " + grbOptimizationModel.get(GRB.IntAttr.NumConstrs));
            System.out.println("Number of binary variables: " + grbOptimizationModel.get(GRB.IntAttr.NumBinVars));
            System.out.println("Number of integer variables: " + grbOptimizationModel.get(GRB.IntAttr.NumIntVars));

            // TODO delete later
            // Check if model is infeasible and log infeasible constraints
            if (grbOptimizationModel.get(GRB.IntAttr.Status) == GRB.Status.INFEASIBLE) {
                logInfeasibleConstraints(grbOptimizationModel, data.instanceName);
            }

            // Print all flow variables
            // printFlowVariables(grbOptimizationModel, data);

            grbOptimizationModel.write("logFile.lp"); // Write the model to a file for debugging
            int[][] startAndFinishTimes = model.getStartAndFinishTimes(grbOptimizationModel, data);

            SolverResults solverResults = buildSolverResults(grbOptimizationModel, targetFunctionValueCurve);
            
            // Create CallbackValues object and populate it with the captured data
            CallbackValues callbackValues = new CallbackValues();
            
            // Add data from targetFunctionValueCurve
            for (Map.Entry<Integer, Integer> entry : targetFunctionValueCurve.entrySet()) {
                callbackValues.addValues(entry.getValue().doubleValue(), entry.getKey().doubleValue(), 1); // 1 indicates it's a solution
            }
            
            // Optionally, add data from detailed callback values
            for (Map<String, Object> callbackEntry : objValueCallback.getCallbackValues()) {
                if ("MIPSOL".equals(callbackEntry.get("type"))) {
                    Double objValue = (Double) callbackEntry.get("objValue");
                    Double time = (Double) callbackEntry.get("time");
                    callbackValues.addValues(objValue, time, 1);
                }
            }
            
            Result result = new Result(new OptimizedSolution(grbOptimizationModel, initialSolution.getModelType()), solverResults, startAndFinishTimes, scheduleResult, data.instanceName, timeComputingAndBuildingHeuristicStartSolution, callbackValues);

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

    private void logInfeasibleConstraints(GRBModel model, String instanceName) {
        try {
            // Compute IIS (Irreducible Inconsistent Subsystem)
            model.computeIIS();
            
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            String infeasibilityLogFile = "infeasible_constraints_" + instanceName + "_" + timestamp + ".log";
            
            try (FileWriter writer = new FileWriter(infeasibilityLogFile)) {
                writer.write("INFEASIBLE MODEL DETECTED\n");
                writer.write("Instance: " + instanceName + "\n");
                writer.write("Timestamp: " + timestamp + "\n");
                writer.write("========================================\n\n");
                
                writer.write("INFEASIBLE CONSTRAINTS (IIS):\n");
                writer.write("------------------------------\n");
                
                // Get all constraints and check which ones are in the IIS
                GRBConstr[] constraints = model.getConstrs();
                int infeasibleCount = 0;
                
                for (GRBConstr constr : constraints) {
                    if (constr.get(GRB.IntAttr.IISConstr) == 1) {
                        writer.write("Constraint: " + constr.get(GRB.StringAttr.ConstrName) + "\n");
                        writer.write("Type: " + (char)constr.get(GRB.CharAttr.Sense) + "\n");
                        writer.write("RHS: " + constr.get(GRB.DoubleAttr.RHS) + "\n");
                        writer.write("---\n");
                        infeasibleCount++;
                    }
                }
                
                writer.write("\nTotal infeasible constraints: " + infeasibleCount + "\n");
                writer.write("\nIIS analysis completed.\n");
            }
            
            System.out.println("Model is infeasible. Infeasible constraints logged to: " + infeasibilityLogFile);
            
        } catch (Exception e) {
            System.err.println("Error while analyzing infeasible constraints: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public SolverResults buildSolverResults(GRBModel model, Map<Integer, Integer> targetFunctionValueCurve) {
        double lowerBound = 0;
        double upperBound = 0;
        double objectiveValue = 0;
        double timeInSeconds = 0;
        double mipGap = 0;
        boolean timeLimitReached = false;

        try {
            // upper lower bound
            lowerBound = model.get(GRB.DoubleAttr.ObjBound); // best bound
            upperBound = model.get(GRB.DoubleAttr.ObjVal); // incumbent
            objectiveValue = model.get(GRB.DoubleAttr.ObjVal);
            timeInSeconds = model.get(GRB.DoubleAttr.Runtime);
            System.out.println("timeInSeconds=" + timeInSeconds);
            mipGap = model.get(GRB.DoubleAttr.MIPGap);
            timeLimitReached = model.get(GRB.IntAttr.Status) == GRB.Status.TIME_LIMIT;
        } catch (Exception e) {
            System.err.println("Error while retrieving solver results: " + e.getMessage());
            e.printStackTrace();
        }
        return new SolverResults(lowerBound, upperBound, objectiveValue, timeInSeconds, mipGap, timeLimitReached, targetFunctionValueCurve);
    }
}