package io;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import enums.PriorityRuleType;
import enums.ModelType;

public class ResultFormatter {
    
    private Map<String, Integer> optimalValues;
    
    public ResultFormatter(Map<String, Integer> optimalValues) {
        this.optimalValues = optimalValues != null ? optimalValues : new HashMap<>();
    }
    
    
    public List<String> formatResults(List<Result> results, Map<String, FileReader.ExistingResultData> existingResults) throws Exception {
        List<String> lines = new ArrayList<>();
        
        // Always add header
        lines.addAll(createHeader());
        
        // Group results by parameter, instance, and model for proper merging
        Map<String, List<Result>> groupedResults = new HashMap<>();
        for (Result result : results) {
            String[] instanceParts = extractInstanceInfo(result.instanceName);
            int parameter = Integer.parseInt(instanceParts[0]);
            int instance = Integer.parseInt(instanceParts[1]);
            String model = getModelShortName(result.getModelType().getDescription());
            String key = parameter + "_" + instance + "_" + model;
            
            List<Result> resultList = groupedResults.get(key);
            if (resultList == null) {
                resultList = new ArrayList<>();
                groupedResults.put(key, resultList);
            }
            resultList.add(result);
        }
        
        int currentParameter = -1;
        int currentInstance = -1;
        
        // Sort the keys
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
            int parameter = Integer.parseInt(parts[0]);
            int instance = Integer.parseInt(parts[1]);
            String model = parts[2];
            
            // Check if this is the first model for this instance
            boolean isFirstModelForInstance = parameter != currentParameter || instance != currentInstance;
            
            // Get results for this key if available
            List<Result> resultsForKey = groupedResults.get(key);
            FileReader.ExistingResultData existingData = existingResults != null ? existingResults.get(key) : null;
            
            Result heuristicResult = null;
            Result nonHeuristicResult = null;
            
            // check if we have heuristic and/or non-heuristic results
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
    
    
    private String formatMergedResultLine(Result heuristicResult, Result nonHeuristicResult,
                                         int parameter, int instance, String model,
                                         boolean isFirstModelForInstance, FileReader.ExistingResultData existingData) {
        
        // Get instance name and optimal makespan
        String instanceName;
        if (heuristicResult != null) {
            instanceName = heuristicResult.instanceName;
        } else {
            instanceName = nonHeuristicResult.instanceName;
        }
        
        Integer optimalMakespan = optimalValues.get(instanceName);
        String optimalStr;
        if (optimalMakespan != null) {
            optimalStr = optimalMakespan.toString();
        } else {
            optimalStr = "N/A";
        }
        
        // Initialize all values to "N/A"
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
            if (heuristicResult.getBestHeuristicMakespan() > 0) {
                heuristicMakespan = String.valueOf(heuristicResult.getBestHeuristicMakespan());
            } else {
                heuristicMakespan = "N/A";
            }
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
        String parameterCol;
        if (isFirstModelForInstance) {
            parameterCol = String.valueOf(parameter);
        } else {
            parameterCol = "";
        }
        
        String instanceCol;
        if (isFirstModelForInstance) {
            instanceCol = String.valueOf(instance);
        } else {
            instanceCol = "";
        }
        
        // Format the final line (i dont even know xd there is propably a better way to do this)
        return String.format("%s\t\t\t%s\t\t\t%s\t%s\t\t\t\t%s\t\t\t\t%s\t\t%s\t\t%s\t\t\t\t\t%s\t%s\t\t%s\t\t%s\t\t\t%s\t\t\t\t%s\t%s",
            parameterCol, instanceCol, model, hMakespan, noHMakespan, hUB, hLB, 
            optimalStr, hTime, noHTime, timeDiff, heuristicMakespan, 
            timeLimitReached, errorStr, heuristicsStr);
    }
    
    // Create header for the result file
    // just dont start a line with a "-", it will break some things in the FileReader and FileWriter
    // cause it is used to indicate the start of the data section
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
        // Try to find matching model type by description
        for (ModelType modelType : ModelType.values()) {
            if (fullModelName.contains(modelType.getDescription()) || 
                fullModelName.contains(modelType.getCode())) {
                return modelType.getCode();
            }
        }
        return fullModelName;
    }
    
    private String getHeuristicShortNames(List<String> heuristics) {
        if (heuristics == null || heuristics.isEmpty()) {
            return "N/A";
        }
        
        List<String> shortNames = new ArrayList<>();
        for (String heuristic : heuristics) {
            // Try to find matching priority rule by description
            String shortName = null;
            for (PriorityRuleType ruleType : PriorityRuleType.values()) {
                if (heuristic.contains(ruleType.getDescription().toUpperCase().replace(" ", "")) ||
                    heuristic.contains(ruleType.getCode())) {
                    shortName = "SSGS-" + ruleType.getCode();
                    break;
                }
            }
            
            if (shortName == null) {
                shortName = heuristic;
            }
            
            if (!shortNames.contains(shortName)) {
                shortNames.add(shortName);
            }
        }
        
        return String.join(", ", shortNames);
    }
    
    private String formatTime(double timeInSeconds) {
        String formattedTime = String.format("%.2f", timeInSeconds);
        return formattedTime;
    }
    
    private boolean isReasonableObjectiveValue(double value) {
        // Check if the value is reasonable (not indicating an infeasible solution)
        // Values larger than 1 billion?? likely indicate infeasible solutions
        // Also check for negative infinity or NaN
        return !Double.isNaN(value) && !Double.isInfinite(value) && value > 0 && value < 1000000000.0;
    }
    
    private boolean checkError(Result result, Integer optimalMakespan) {
        if (optimalMakespan == null) return false;
        double computedMakespan = result.solverResults.objectiveValue;
        return computedMakespan < optimalMakespan.doubleValue();
    }
}
