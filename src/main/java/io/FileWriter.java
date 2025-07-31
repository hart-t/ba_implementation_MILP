package io;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileWriter {
    
    private Map<String, Integer> optimalValues;
    private Map<String, String> existingResults; // Maps "parameter_instance_model" to full line
    
    public FileWriter() {
        this.optimalValues = new HashMap<>();
        this.existingResults = new HashMap<>();
        loadOptimalValues();
    }
    
    private void loadOptimalValues() {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                new FileInputStream("/home/tobsi/university/kit/RCPSP_Benchmark_Solutions/j30opt.sm/j30opt.sm")))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                // Skip empty lines and header lines
                if (line.isEmpty() || line.startsWith("=") || line.startsWith("Authors") || 
                    line.startsWith("Instance") || line.startsWith("Type") || line.startsWith("Date") ||
                    line.startsWith("Research") || line.startsWith("Computer") || line.startsWith("Processor") ||
                    line.startsWith("Clockpulse") || line.startsWith("Operating") || line.startsWith("Memory") ||
                    line.startsWith("Language") || line.startsWith("Average") || line.startsWith("Paramter") ||
                    line.startsWith("--") || line.contains("benchmark") || line.contains("resource-constrained")) {
                    continue;
                }
                
                String[] parts = line.split("\\s+");
                if (parts.length >= 3) {
                    try {
                        int parameter = Integer.parseInt(parts[0]);
                        int instance = Integer.parseInt(parts[1]);
                        int makespan = Integer.parseInt(parts[2]);
                        
                        // Create key in format j30{parameter}_{instance}
                        String key = String.format("j30%d_%d", parameter, instance);
                        optimalValues.put(key, makespan);
                    } catch (NumberFormatException e) {
                        // Skip lines that don't have valid numbers
                        continue;
                    }
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
        
        // Check if file exists and load existing results
        boolean fileExists = file.exists();
        if (fileExists) {
            loadExistingResults(file);
            checkForDuplicates(results);
        }
        
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, fileExists)))) {
            if (!fileExists) {
                writeHeader(writer);
            }
            
            String currentParameter = "";
            String currentInstance = "";
            
            for (Result result : results) {
                String[] instanceParts = extractInstanceInfo(result.instanceName);
                String parameter = instanceParts[0];
                String instance = instanceParts[1];
                
                writeResultRow(writer, result, parameter, instance, currentParameter, currentInstance);
                
                currentParameter = parameter;
                currentInstance = instance;
            }
        }
    }
    
    private void loadExistingResults(File file) throws Exception {
        existingResults.clear();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
            String line;
            String currentParameter = "";
            String currentInstance = "";
            
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                
                // Skip header lines and empty lines
                if (line.isEmpty() || line.startsWith("=") || line.startsWith("Instance Set") || 
                    line.startsWith("Type") || line.startsWith("Paramter") || line.startsWith("-")) {
                    continue;
                }
                
                // Parse data lines
                String[] parts = line.split("\\s+");
                if (parts.length >= 3) {
                    String parameter = parts[0].trim();
                    String instance = parts[1].trim();
                    String model = parts[2].trim();
                    
                    // Check if this is a continuation line (empty parameter/instance)
                    if (!parameter.isEmpty() && !instance.isEmpty()) {
                        // This is a new instance line
                        currentParameter = parameter;
                        currentInstance = instance;
                        String key = parameter + "_" + instance + "_" + model;
                        existingResults.put(key, line);
                    } else if (parts.length >= 1 && !parts[0].trim().isEmpty()) {
                        // This is a continuation line with just model name
                        model = parts[0].trim();
                        // Use last known parameter and instance
                        if (!currentParameter.isEmpty() && !currentInstance.isEmpty()) {
                            String key = currentParameter + "_" + currentInstance + "_" + model;
                            existingResults.put(key, line);
                        }
                    }
                }
            }
        }
    }
    
    private void checkForDuplicates(List<Result> results) throws Exception {
        for (Result result : results) {
            String[] instanceParts = extractInstanceInfo(result.instanceName);
            String parameter = instanceParts[0];
            String instance = instanceParts[1];
            String model = getModelShortName(result.getModelType().getDescription());
            
            String key = parameter + "_" + instance + "_" + model;
            
            if (existingResults.containsKey(key)) {
                throw new Exception("ERROR: Instance " + parameter + "_" + instance + 
                                  " with model " + model + " already exists in the results file. " +
                                  "Cannot overwrite existing results. Please remove the existing entry or use a different file.");
            }
        }
    }
    
    private void writeHeader(BufferedWriter writer) throws Exception {
        writer.write("===================================================================================================================================================================================");
        writer.newLine();
        writer.write("Instance Set            : j30");
        writer.newLine();
        writer.write("Type                    : sm");
        writer.newLine();
        writer.newLine();
        writer.write("===================================================================================================================================================================================");
        writer.newLine();
        writer.write("Paramter Instance Model\tModel-Makespan  UB  LB  MIP-Gap Optimal-Makespan Time Heuristic-Makespan Stopped Error Heuristics");
        writer.newLine();
        writer.write("-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------");
        writer.newLine();
    }
    
    private String[] extractInstanceInfo(String instanceName) {
        // Extract parameter and instance from instanceName (e.g., "j301_1" -> ["1", "1"])
        if (instanceName.startsWith("j30")) {
            String remaining = instanceName.substring(3); // Remove "j30"
            String[] parts = remaining.split("_");
            if (parts.length >= 2) {
                return new String[]{parts[0], parts[1]};
            }
        }
        return new String[]{"", ""};
    }

    private void writeResultRow(BufferedWriter writer, Result result, String parameter, String instance, 
                              String currentParameter, String currentInstance) throws Exception {
        
        boolean isFirstModelForInstance = !parameter.equals(currentParameter) || !instance.equals(currentInstance);
        
        // Convert model name to short form
        String modelShortName = getModelShortName(result.getModelType().getDescription());
        
        if (isFirstModelForInstance) {
            writer.write(String.format("%-8s%-10s%-10s\t", parameter, instance, modelShortName));
        } else {
            writer.write(String.format("%-8s%-10s%-10s\t", "", "", modelShortName));
        }
        
        // Format time to max 2 decimal places
        String formattedTime = String.format("%.2f", result.solverResults.timeInSeconds);
        if (formattedTime.endsWith(".00")) {
            formattedTime = formattedTime.substring(0, formattedTime.length() - 3);
        }
        
        // Get optimal makespan
        Integer optimalMakespan = optimalValues.get(result.instanceName);
        String optimalStr = optimalMakespan != null ? optimalMakespan.toString() : "N/A";
        
        // Get heuristic info
        String heuristicMakespan = result.getBestHeuristicMakespan() > 0 ? 
            String.valueOf(result.getBestHeuristicMakespan()) : "N/A";
        
        // Convert heuristic names to short form
        String heuristics = getHeuristicShortNames(result.getUsedHeuristics());
        
        // Determine if stopped (assuming there's a time limit check in Result)
        boolean stopped = result.solverResults.wasStoppedByTimeLimit();
        
        // Determine if error (computed makespan below optimal) - use precise comparison
        boolean error = false;
        if (optimalMakespan != null) {
            double computedMakespan = result.solverResults.objectiveValue;
            error = computedMakespan < optimalMakespan.doubleValue();
        }
        
        // Get MIP-Gap from solver results
        double mipGap = result.solverResults.getMipGap(); // Assuming this method exists
        
        writer.write(String.format("%-6.0f\t%-3.0f %-3.0f %-7.2f         %-8s         %-6s\t      %-10s %-7s %-7s %-4s",
            result.solverResults.objectiveValue,
            result.solverResults.upperBound,
            result.solverResults.lowerBound,
            mipGap,
            optimalStr,
            formattedTime,
            heuristicMakespan,
            stopped,
            error,
            heuristics
            ));
        writer.newLine();
    }
    
    private String getModelShortName(String fullModelName) {
        if (fullModelName.contains("Flow-Based") || fullModelName.contains("FLOW")) {
            return "FLOW";
        } else if (fullModelName.contains("Discrete") || fullModelName.contains("DISC")) {
            return "DISC";
        } else if (fullModelName.contains("On-Off") || fullModelName.contains("EVENT") || fullModelName.contains("ONOFF")) {
            return "ONOFF";
        }
        return fullModelName; // fallback
    }
    
    private String getHeuristicShortNames(List<String> heuristics) {
        if (heuristics == null || heuristics.isEmpty()) {
            return "None";
        }
        
        List<String> shortNames = new ArrayList<>();
        for (String heuristic : heuristics) {
            String shortName = convertHeuristicToShortName(heuristic);
            if (!shortNames.contains(shortName)) { // avoid duplicates
                shortNames.add(shortName);
            }
        }
        
        return String.join(", ", shortNames);
    }
    
    private String convertHeuristicToShortName(String fullHeuristicName) {
        // Convert long heuristic names to short abbreviations
        if (fullHeuristicName.contains("SHORTESTPROCESSINGTIME") || fullHeuristicName.contains("SPT")) {
            return "SSGS-SPT";
        } else if (fullHeuristicName.contains("GREATESTRANKPOSITIONALWEIGHT") || fullHeuristicName.contains("GRPW")) {
            return "SSGS-GRPW";
        } else if (fullHeuristicName.contains("MOSTRESOURCEUSAGE") || fullHeuristicName.contains("MRU")) {
            return "SSGS-MRU";
        } else if (fullHeuristicName.contains("RESOURCESCHEDULINGMETHOD") || fullHeuristicName.contains("RSM")) {
            return "SSGS-RSM";
        } else if (fullHeuristicName.contains("MOSTTOTALSUCCESSORS") || fullHeuristicName.contains("MTS")) {
            return "SSGS-MTS";
        } else if (fullHeuristicName.contains("MINIMUMLATESTSTARTTIME") || fullHeuristicName.contains("MLST")) {
            return "SSGS-MLST";
        } else if (fullHeuristicName.contains("MINIMUMLATESTFINISHTIME") || fullHeuristicName.contains("MLFT")) {
            return "SSGS-MLFT";
        } else if (fullHeuristicName.contains("MINIMUMJOBSLACK") || fullHeuristicName.contains("MJS")) {
            return "SSGS-MJS";
        }
        return fullHeuristicName; // fallback
    }
    
}