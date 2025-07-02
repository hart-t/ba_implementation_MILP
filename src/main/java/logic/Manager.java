package logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gurobi.gurobi.GRB;
import com.gurobi.gurobi.GRBEnv;
import com.gurobi.gurobi.GRBException;
import com.gurobi.gurobi.GRBModel;
import com.gurobi.gurobi.GRBVar;

import io.FileReader;
import io.JobDataInstance;
import io.Result;
import models.DiscreteTimeModel;
import models.FlowBasedContinuousTimeModel;
import models.HeuristicSerialSGS;

public class Manager {
    public static void runModels() {
        //File[] files = new File("/home/tobsi/university/kit/benchmarkSets").listFiles();
        //assert files != null;
        //String filename = "/home/tobsi/university/kit/benchmarkSets/" + files[0].getName();
        String filename = "/home/tobsi/university/kit/benchmarkSets/j303_3.sm";
        String modelName = "";

        // enter Model name here
        //modelName = "FlowBasedContinuousTimeModel";
        modelName = "DiscreteTimeModel";
        //modelName = "OnOffEventBasedModel";
        boolean heuristicUsed = false;

        try {

            // Create FileReader instance and get the data
            FileReader fileReader = new FileReader();
            JobDataInstance data = fileReader.dataRead(filename);

            // Initialize the Gurobi environment and model
            GRBEnv env = new GRBEnv();
            GRBModel model = new GRBModel(env);








            System.out.println("Using model: " + modelName);
            System.out.println("Using file: " + filename);
            if (modelName.equals("FlowBasedContinuousTimeModel")) {
                model = FlowBasedContinuousTimeModel.gurobiRcpspJ30(model, data);
            } else if (modelName.equals("DiscreteTimeModel")) {
                model = DiscreteTimeModel.gurobiRcpspJ30(model, data);
            } else {
                throw new IllegalArgumentException("Unknown model name: " + modelName);  
            }

            if (heuristicUsed == true) {
            // Apply heuristic solution to model
            Map<Integer, Integer> heuristicSolution = HeuristicSerialSGS.serialSGS(data);
            //applySolutionWithGurobi(model, data.numberJob, data.jobDuration, heuristicSolution);
            // Debug: Check if we're using correct heuristic solution
            System.out.println("Heuristic solution makespan: " + heuristicSolution.get(data.numberJob - 1));
            System.out.println("Heuristic start times: " + heuristicSolution);
            }
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
            
            // Print solution information
            if (model.get(GRB.IntAttr.Status) == GRB.Status.OPTIMAL) {
                System.out.println("Optimal solution found!");
                System.out.println("Objective value: " + model.get(GRB.DoubleAttr.ObjVal));
            } else if (model.get(GRB.IntAttr.Status) == GRB.Status.TIME_LIMIT) {
                System.out.println("Time limit reached. Best solution: " + model.get(GRB.DoubleAttr.ObjVal));
                System.out.println("MIP Gap: " + model.get(GRB.DoubleAttr.MIPGap));
            } else if (model.get(GRB.IntAttr.Status) == GRB.Status.INFEASIBLE) {
                System.out.println("Model is infeasible.");
                model.computeIIS();
                model.write("model.ilp");
            } else {
                System.out.println("Solution status: " + model.get(GRB.IntAttr.Status));
            }

            if (modelName.equals("FlowBasedContinuousTimeModel")) {
                    Result.printResult(fillDoubleListsToReturn(model, data.jobDuration, data.numberJob));
            } else if (modelName.equals("OnOffEventBasedModel")) {
                    Result.printResult(fillDoubleListsToReturn(model, data.jobDuration, data.numberJob));
            } else {
            Result.printResult(fillIntegerListsToReturn(model, data.jobDuration, data.numberJob));
            }

            // Clean up Gurobi model and environment
            model.dispose();
            env.dispose();


            } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Result.ScheduleIntegerResult fillIntegerListsToReturn(GRBModel model, List<Integer> jobDuration, int numJob) throws GRBException {

        // Safe solution in outputDict
        Map<Integer, Integer> outputDict = new HashMap<>();
        for (GRBVar var : model.getVars()) {
            if (var.get(GRB.DoubleAttr.X) > 0.5) { // Use > 0.5 for binary variables
                System.out.println(var.get(GRB.StringAttr.VarName) + " " + var.get(GRB.DoubleAttr.X));
                String varName = var.get(GRB.StringAttr.VarName);
                
                // Parse variable name: "startingTime[i] at [t]"
                if (varName.startsWith("startingTime[")) {
                    String[] parts = varName.split("\\] at \\[");
                    if (parts.length == 2) {
                        int jobId = Integer.parseInt(parts[0].substring("startingTime[".length()));
                        int startTime = Integer.parseInt(parts[1].substring(0, parts[1].length() - 1));
                        outputDict.put(jobId, startTime);
                    }
                }
            }
        }

        List<Integer> start = new ArrayList<>(numJob);
        List<Integer> finish = new ArrayList<>(numJob);

        for (int i = 0; i < numJob; i++) {
            start.add(0);
            finish.add(0);
        }

        // Fill start times
        for (Map.Entry<Integer, Integer> entry : outputDict.entrySet()) {
            start.set(entry.getKey(), entry.getValue());
        }

        // Fill finish times
        for (int i = 0; i < numJob; i++) {
            finish.set(i, start.get(i) + jobDuration.get(i));
        }
        return new Result.ScheduleIntegerResult(start, finish);
    }

    private static Result.ScheduleDoubleResult fillDoubleListsToReturn(GRBModel model, List<Integer> jobDuration, int numJob) throws GRBException {
        Map<Integer, Double> outputDict = new HashMap<>();
        
        // Only extract start time variables, not on-off variables
        for (GRBVar var : model.getVars()) {
            String varName = var.get(GRB.StringAttr.VarName);
            if (varName.startsWith("startingTime[")) {
                // Extract job index from variable name "startingTime[i]"
                int jobIndex = Integer.parseInt(varName.substring("startingTime[".length(), varName.indexOf(']')));
                double startTime = var.get(GRB.DoubleAttr.X);
                System.out.println(varName + " " + startTime); // This will show start times, not y variables
                outputDict.put(jobIndex, startTime);
            }
        }

        List<Double> start = new ArrayList<>(numJob);
        List<Double> finish = new ArrayList<>(numJob);

        for (int i = 0; i < numJob; i++) {
            start.add(outputDict.getOrDefault(i, 0.0));
            finish.add(start.get(i) + jobDuration.get(i));
        }

        // Integer rounding
        for (int i = 0; i < numJob; i++) {
            double startTime = start.get(i);
            double finishTime = finish.get(i);

            if (Math.abs(startTime - Math.round(startTime)) < 0.01) {
                startTime = Math.round(startTime);
            }
            if (Math.abs(finishTime - Math.round(finishTime)) < 0.01) {
                finishTime = Math.round(finishTime);
            }

            start.set(i, startTime);
            finish.set(i, finishTime);
        }

        return new Result.ScheduleDoubleResult(start, finish);
    }
}
