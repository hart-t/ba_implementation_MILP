package io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResultFormatter {
    
    private Map<String, Integer> optimalValues;
    private Map<String, ResultData> existingResults; // Maps "parameter_instance_model" to existing data
    
    public ResultFormatter() {
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
                        
                        String key = String.format("j30%d_%d", parameter, instance);
                        optimalValues.put(key, makespan);
                    } catch (NumberFormatException e) {
                        continue;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not load optimal values file: " + e.getMessage());
        }
    }
    
    public List<String> formatResults(List<Result> results, File outputFile) throws Exception {
        List<String> lines = new ArrayList<>();
        
        // Check if file exists and load existing data if it does
        boolean fileExists = outputFile.exists();
        if (fileExists) {
            loadExistingResults(outputFile);
        }
        
        // Always add header (either for new file or when rewriting existing file)
        lines.addAll(createHeader());
        
        // Group results by parameter, instance, and model for proper merging
        Map<String, List<Result>> groupedResults = new HashMap<>();
        for (Result result : results) {
            String[] instanceParts = extractInstanceInfo(result.instanceName);
            String parameter = instanceParts[0];
            String instance = instanceParts[1];
            String model = getModelShortName(result.getModelType().getDescription());
            String key = parameter + "_" + instance + "_" + model;
            
            List<Result> resultList = groupedResults.get(key);
            if (resultList == null) {
                resultList = new ArrayList<>();
                groupedResults.put(key, resultList);
            }
            resultList.add(result);
        }
        
        // Process results in sorted order
        String currentParameter = "";
        String currentInstance = "";
        
        // Sort the keys to ensure proper ordering
        List<String> sortedKeys = new ArrayList<>(groupedResults.keySet());
        sortedKeys.sort((k1, k2) -> {
            String[] parts1 = k1.split("_");
            String[] parts2 = k2.split("_");
            
            // Compare parameter first
            int param1 = Integer.parseInt(parts1[0]);
            int param2 = Integer.parseInt(parts2[0]);
            if (param1 != param2) return Integer.compare(param1, param2);
            
            // Then instance
            int inst1 = Integer.parseInt(parts1[1]);
            int inst2 = Integer.parseInt(parts2[1]);
            if (inst1 != inst2) return Integer.compare(inst1, inst2);
            
            // Finally model
            return parts1[2].compareTo(parts2[2]);
        });
        
        for (String key : sortedKeys) {
            String[] parts = key.split("_");
            String parameter = parts[0];
            String instance = parts[1];
            String model = parts[2];
            
            boolean isFirstModelForInstance = !parameter.equals(currentParameter) || !instance.equals(currentInstance);
            
            List<Result> resultsForKey = groupedResults.get(key);
            ResultData existingData = existingResults.get(key);
            
            // Merge all results for this key (heuristic and non-heuristic)
            Result heuristicResult = null;
            Result nonHeuristicResult = null;
            
            for (Result result : resultsForKey) {
                boolean hasHeuristics = result.getUsedHeuristics() != null && !result.getUsedHeuristics().isEmpty();
                if (hasHeuristics) {
                    heuristicResult = result;
                } else {
                    nonHeuristicResult = result;
                }
            }
            
            // Create merged line
            String formattedLine = formatMergedResultLine(heuristicResult, nonHeuristicResult, 
                                                         parameter, instance, model, 
                                                         isFirstModelForInstance, existingData);
            lines.add(formattedLine);
            
            currentParameter = parameter;
            currentInstance = instance;
        }
        
        return lines;
    }
    
    private void loadExistingResults(File file) throws Exception {
        existingResults.clear();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
            String line;
            String currentParameter = "";
            String currentInstance = "";
            
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                
                // Skip header lines
                if (line.isEmpty() || line.startsWith("=") || line.startsWith("Parameter") || 
                    line.startsWith("Instance") || line.startsWith("Model") || line.startsWith("H_M_Makespan") ||
                    line.startsWith("Paramter") || line.startsWith("-")) {
                    continue;
                }
                
                // Parse data lines
                String[] parts = line.split("\\s+");
                if (parts.length >= 3) {
                    String parameter = parts[0].trim();
                    String instance = parts[1].trim();
                    String model = parts[2].trim();
                    
                    if (!parameter.isEmpty() && !instance.isEmpty()) {
                        currentParameter = parameter;
                        currentInstance = instance;
                    } else if (!parts[0].trim().isEmpty()) {
                        model = parts[0].trim();
                        parameter = currentParameter;
                        instance = currentInstance;
                    }
                    
                    if (!parameter.isEmpty() && !instance.isEmpty()) {
                        String key = parameter + "_" + instance + "_" + model;
                        ResultData data = parseExistingLine(parts);
                        existingResults.put(key, data);
                    }
                }
            }
        }
    }
    
    private ResultData parseExistingLine(String[] parts) {
        ResultData data = new ResultData();
        
        if (parts.length > 3) data.hMakespan = parseValue(parts[3]);
        if (parts.length > 4) data.noHMakespan = parseValue(parts[4]);
        if (parts.length > 5) data.hUB = parseValue(parts[5]);
        if (parts.length > 6) data.hLB = parseValue(parts[6]);
        // Skip optimalMakespan as it's always recalculated
        if (parts.length > 8) data.hTime = parseValue(parts[8]);
        if (parts.length > 9) data.noHTime = parseValue(parts[9]);
        // Skip timeDiff as it's always recalculated
        if (parts.length > 11) data.heuristicMakespan = parseValue(parts[11]);
        if (parts.length > 12) data.timeLimitReached = parseValue(parts[12]);
        if (parts.length > 13) data.error = parseValue(parts[13]);
        if (parts.length > 14) {
            StringBuilder heuristics = new StringBuilder();
            for (int i = 14; i < parts.length; i++) {
                if (i > 14) heuristics.append(" ");
                heuristics.append(parts[i]);
            }
            data.heuristics = heuristics.toString();
        }
        
        return data;
    }
    
    private String parseValue(String value) {
        return "N/A".equals(value) ? null : value;
    }
    
    private String formatMergedResultLine(Result heuristicResult, Result nonHeuristicResult,
                                         String parameter, String instance, String model,
                                         boolean isFirstModelForInstance, ResultData existingData) {
        
        // Get optimal makespan for this instance
        String instanceName = heuristicResult != null ? heuristicResult.instanceName : nonHeuristicResult.instanceName;
        Integer optimalMakespan = optimalValues.get(instanceName);
        String optimalStr = optimalMakespan != null ? optimalMakespan.toString() : "N/A";
        
        // Initialize all values
        String hMakespan = "N/A";
        String noHMakespan = "N/A";
        String hUB = "N/A";
        String hLB = "N/A";
        String hTime = "N/A";
        String noHTime = "N/A";
        String timeDiff = "N/A";
        String heuristicMakespan = "N/A";
        String timeLimitReached = "N/A";
        String errorStr = "N/A";
        String heuristicsStr = "N/A";
        
        // Fill values from heuristic result
        if (heuristicResult != null) {
            // Check if objective value is reasonable (not indicating infeasible solution)
            if (isReasonableObjectiveValue(heuristicResult.solverResults.objectiveValue)) {
                hMakespan = String.valueOf((int)heuristicResult.solverResults.objectiveValue);
            } else {
                hMakespan = "INFEASIBLE";
            }
            
            // Check bounds similarly
            if (isReasonableObjectiveValue(heuristicResult.solverResults.upperBound)) {
                hUB = String.valueOf((int)heuristicResult.solverResults.upperBound);
            } else {
                hUB = "INFEASIBLE";
            }
            
            if (isReasonableObjectiveValue(heuristicResult.solverResults.lowerBound)) {
                hLB = String.valueOf((int)heuristicResult.solverResults.lowerBound);
            } else {
                hLB = "INFEASIBLE";
            }
            
            hTime = formatTime(heuristicResult.solverResults.timeInSeconds);
            heuristicMakespan = heuristicResult.getBestHeuristicMakespan() > 0 ? 
                String.valueOf(heuristicResult.getBestHeuristicMakespan()) : "N/A";
            timeLimitReached = String.valueOf(heuristicResult.solverResults.wasStoppedByTimeLimit());
            errorStr = String.valueOf(checkError(heuristicResult, optimalMakespan));
            heuristicsStr = getHeuristicShortNames(heuristicResult.getUsedHeuristics());
        }
        
        // Fill values from non-heuristic result
        if (nonHeuristicResult != null) {
            // Check if objective value is reasonable (not indicating infeasible solution)
            if (isReasonableObjectiveValue(nonHeuristicResult.solverResults.objectiveValue)) {
                noHMakespan = String.valueOf((int)nonHeuristicResult.solverResults.objectiveValue);
            } else {
                noHMakespan = "INFEASIBLE";
            }
            
            noHTime = formatTime(nonHeuristicResult.solverResults.timeInSeconds);
            // Update error if we don't have it from heuristic result
            if ("N/A".equals(errorStr)) {
                errorStr = String.valueOf(checkError(nonHeuristicResult, optimalMakespan));
            }
        }
        
        // Merge with existing data if available
        if (existingData != null) {
            if (existingData.hMakespan != null) hMakespan = existingData.hMakespan;
            if (existingData.noHMakespan != null) noHMakespan = existingData.noHMakespan;
            if (existingData.hUB != null) hUB = existingData.hUB;
            if (existingData.hLB != null) hLB = existingData.hLB;
            if (existingData.hTime != null) hTime = existingData.hTime;
            if (existingData.noHTime != null) noHTime = existingData.noHTime;
            if (existingData.heuristicMakespan != null) heuristicMakespan = existingData.heuristicMakespan;
            if (existingData.timeLimitReached != null) timeLimitReached = existingData.timeLimitReached;
            if (existingData.error != null) errorStr = existingData.error;
            if (existingData.heuristics != null) heuristicsStr = existingData.heuristics;
        }
        
        // Calculate time difference if both times are available
        if (!"N/A".equals(hTime) && !"N/A".equals(noHTime)) {
            try {
                double hTimeVal = Double.parseDouble(hTime);
                double noHTimeVal = Double.parseDouble(noHTime);
                double diff = hTimeVal - noHTimeVal;
                timeDiff = formatTime(diff);
            } catch (NumberFormatException e) {
                timeDiff = "N/A";
            }
        }
        
        // Format parameter and instance columns
        String parameterCol = isFirstModelForInstance ? parameter : "";
        String instanceCol = isFirstModelForInstance ? instance : "";
        
        return String.format("%s\t\t\t%s\t\t\t%s\t%s\t\t\t\t%s\t\t\t\t%s\t\t%s\t\t%s\t\t\t\t\t%s\t%s\t\t%s\t\t%s\t\t\t%s\t\t\t\t%s\t%s",
            parameterCol, instanceCol, model, hMakespan, noHMakespan, hUB, hLB, 
            optimalStr, hTime, noHTime, timeDiff, heuristicMakespan, 
            timeLimitReached, errorStr, heuristicsStr);
    }
    
    
    private List<String> createHeader() {
        List<String> header = new ArrayList<>();
        header.add("===================================================================================================================================================================================");
        header.add("Instance Set            : j30");
        header.add("Type                    : sm");
        header.add("");
        header.add("Parameter\t\t\t: parameter number");
        header.add("Instance\t\t\t: instance number");
        header.add("Model\t\t\t\t: abbrevation of the used model");
        header.add("H_M_Makespan\t\t: the computed makespan of the model if at least one heuristic start solution is used");
        header.add("noH_Makespan\t\t: the computed makespan of the model if no heuristic start solution is used");
        header.add("H_UB\t\t\t\t: upper bound if at least one heuristic start solution is used");
        header.add("H_LB\t\t\t\t: lower bound if at least one heuristic start solution is used");
        header.add("Optimal-Makespan\t: the optimal makespan of the problem-instance from the official solution file");
        header.add("H_Time\t\t\t\t: the time it took the model to compute the problem instance if at least one heuristic start solution is used");
        header.add("noH_Time\t\t\t: the time it took the model to compute the problem instance if no heuristic start solution is used");
        header.add("Time_Diff\t\t\t: the time diffrence it took the model to compute with or without the use of a heuristic start solution (H-Time - noH-Time)");
        header.add("H_Makespan\t\t\t: the makespan of the heuristic start solution, if one is used");
        header.add("time_limit_reached\t: if the solver was stopped cause the given time limit to solve the problem instance is reached");
        header.add("Error\t\t\t\t: if the optimal makespan is not within the computed bounds by either the use of the model with or without the use of a heuristic start solution");
        header.add("Heuristics\t\t\t: a list of the abbrevations of the used heuristic start solutions");
        header.add("");
        header.add("Total time the models with the heuristic start solution were able to compute the problem instances faster than the models without the heuristic start solution: ");
        header.add("count every instance: ");
        header.add("average time difference: ");
        header.add("count only the instances where the time limit was not reached: ");
        header.add("average time difference: ");
        header.add("===================================================================================================================================================================================");
        header.add("Paramter\tInstance\tModel\tH_M_Makespan\tnoH_M_Makespan\tH_UB\tH_LB\tOptimal_Makespan\tH_Time\tnoH_Time\tTime_Diff\tH_Makespan\ttime_limit_reached\tError\tHeuristics");
        header.add("-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------");
        return header;
    }
    
    private String[] extractInstanceInfo(String instanceName) {
        if (instanceName.startsWith("j30")) {
            String remaining = instanceName.substring(3);
            String[] parts = remaining.split("_");
            if (parts.length >= 2) {
                return new String[]{parts[0], parts[1]};
            }
        }
        return new String[]{"", ""};
    }
    
    private String getModelShortName(String fullModelName) {
        if (fullModelName.contains("Flow-Based") || fullModelName.contains("FLOW")) {
            return "FLOW";
        } else if (fullModelName.contains("Discrete") || fullModelName.contains("DISC")) {
            return "DISC";
        } else if (fullModelName.contains("On-Off") || fullModelName.contains("EVENT") || fullModelName.contains("ONOFF")) {
            return "ONOFF";
        } else if (fullModelName.contains("Interval") || fullModelName.contains("IEE")) {
            return "IEE";
        }
        return fullModelName;
    }
    
    private String getHeuristicShortNames(List<String> heuristics) {
        if (heuristics == null || heuristics.isEmpty()) {
            return "N/A";
        }
        
        List<String> shortNames = new ArrayList<>();
        for (String heuristic : heuristics) {
            String shortName = convertHeuristicToShortName(heuristic);
            if (!shortNames.contains(shortName)) {
                shortNames.add(shortName);
            }
        }
        
        return String.join(", ", shortNames);
    }
    
    private String convertHeuristicToShortName(String fullHeuristicName) {
        if (fullHeuristicName.contains("SHORTESTPROCESSINGTIME") || fullHeuristicName.contains("SPT")) {
            return "SSGS-SPT";
        } else if (fullHeuristicName.contains("GREATESTRANKPOSITIONALWEIGHT") || fullHeuristicName.contains("GRPW") || fullHeuristicName.contains("RPW")) {
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
        return fullHeuristicName;
    }
    
    private String formatTime(double timeInSeconds) {
        String formattedTime = String.format("%.2f", timeInSeconds);
        return formattedTime;
    }
    
    private boolean isReasonableObjectiveValue(double value) {
        // Check if the value is reasonable (not indicating an infeasible solution)
        // Values larger than 1 billion likely indicate infeasible solutions
        // Also check for negative infinity or NaN
        return !Double.isNaN(value) && !Double.isInfinite(value) && value > 0 && value < 1000000000.0;
    }
    
    private boolean checkError(Result result, Integer optimalMakespan) {
        if (optimalMakespan == null) return false;
        double computedMakespan = result.solverResults.objectiveValue;
        return computedMakespan < optimalMakespan.doubleValue();
    }
    
    // Helper class to store existing result data
    private static class ResultData {
        String hMakespan;
        String noHMakespan;
        String hUB;
        String hLB;
        String hTime;
        String noHTime;
        String heuristicMakespan;
        String timeLimitReached;
        String error;
        String heuristics;
    }
}
