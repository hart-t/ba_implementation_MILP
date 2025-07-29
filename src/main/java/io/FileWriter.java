package io;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

public class FileWriter {
    
    public void writeResults(String directory, String filename, List<Result> results, String instanceName, String modelName) throws Exception {
        // Create directory if it doesn't exist
        File dir = new File(directory);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        // Create full file path
        File file = new File(dir, filename);
        
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)))) {
            for (Result result : results) {
                writeResult(writer, result, instanceName, result.getModelType().getDescription());
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
    }
}
