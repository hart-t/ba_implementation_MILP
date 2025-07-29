package io;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileWriter {
    
    private Map<String, Integer> optimalValues;
    
    public FileWriter() {
        this.optimalValues = new HashMap<>();
        loadOptimalValues();
    }
    
    private void loadOptimalValues() {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                new FileInputStream("/home/tobsi/university/kit/RCPSP_Benchmark_Solutions/j30opt.sm")))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length >= 2) {
                    String instanceName = parts[0];
                    int optimalMakespan = Integer.parseInt(parts[1]);
                    optimalValues.put(instanceName, optimalMakespan);
                }
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not load optimal values file: " + e.getMessage());
        }
    }
    
    public void writeResults(String directory, String filename, List<Result> results) throws Exception {
        // Create directory if it doesn't exist
        File dir = new File(directory);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        // Create full file path
        File file = new File(dir, filename);
        
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)))) {
            for (Result result : results) {
                writeResult(writer, result, result.instanceName, result.getModelType().getDescription());
                writer.newLine(); // Add empty line between results
            }
        }
    }
    
    private void writeResult(BufferedWriter writer, Result result, String instanceName, String modelName) throws Exception {
        writer.write("Instance Name: " + instanceName);
        writer.newLine();
        
        writer.write("Model Name: " + modelName);
        writer.newLine();
        
        // Write heuristics used (if any)
        List<String> heuristics = result.getUsedHeuristics();
        if (heuristics != null && !heuristics.isEmpty()) {
            writer.write("Heuristics Used: " + String.join(", ", heuristics));
            writer.newLine();
            
            writer.write("Best Heuristic Makespan: " + result.getBestHeuristicMakespan());
            writer.newLine();
        } else {
            writer.write("Heuristics Used: None");
            writer.newLine();
            
            writer.write("Best Heuristic Makespan: N/A");
            writer.newLine();
        }
        
        // Write solver results
        writer.write("Computed Makespan (Objective Value): " + result.solverResults.objectiveValue);
        writer.newLine();
        
        writer.write("Upper Bound: " + result.solverResults.upperBound);
        writer.newLine();
        
        writer.write("Lower Bound: " + result.solverResults.lowerBound);
        writer.newLine();
        
        writer.write("Computation Time (seconds): " + result.solverResults.timeInSeconds);
        writer.newLine();
        
        // Validate against optimal values
        validateResult(writer, result, instanceName);
    }
    
    private void validateResult(BufferedWriter writer, Result result, String instanceName) throws Exception {
        Integer optimalMakespan = optimalValues.get(instanceName);
        if (optimalMakespan != null) {
            double computedMakespan = result.solverResults.objectiveValue;
            double lowerBound = result.solverResults.lowerBound;
            double upperBound = result.solverResults.upperBound;
            
            writer.write("Optimal Makespan (from solution file): " + optimalMakespan);
            writer.newLine();
            
            // Check for inconsistencies
            if (computedMakespan < optimalMakespan) {
                writer.write("WARNING: Computed makespan (" + computedMakespan + ") is below known optimal makespan (" + optimalMakespan + ")!");
                writer.newLine();
            }
            
            if (computedMakespan < lowerBound) {
                writer.write("WARNING: Computed makespan (" + computedMakespan + ") is below lower bound (" + lowerBound + ")!");
                writer.newLine();
            }
            
            if (computedMakespan > upperBound && upperBound > 0) {
                writer.write("WARNING: Computed makespan (" + computedMakespan + ") is above upper bound (" + upperBound + ")!");
                writer.newLine();
            }
            
            if (computedMakespan == optimalMakespan) {
                writer.write("SUCCESS: Computed makespan matches optimal value!");
                writer.newLine();
            }
        } else {
            writer.write("WARNING: No optimal makespan found for instance " + instanceName + " in solution file!");
            writer.newLine();
        }
    }
}
