package io;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import enums.ModelType;

public class FileReader {

    public JobDataInstance dataRead(String file) throws Exception {
        System.out.println("Reading file: " + file);
        List<String[]> data = new ArrayList<>();
        
        // Read file and split lines
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.trim().split("\\s+");
                data.add(parts);
            }
        }

        List<Integer> jobNumSuccessors = new ArrayList<>();
        List<List<Integer>> jobSuccessors = new ArrayList<>();
        List<List<Integer>> jobPredecessors = new ArrayList<>();
        List<Integer> jobDuration = new ArrayList<>();
        List<List<Integer>> jobResource = new ArrayList<>();

        int numberJob = Integer.parseInt(data.get(5)[4]);
        int horizon = Integer.parseInt(data.get(6)[2]);
        List<Integer> resourceCapacity = Arrays.stream(data.get(89))
                                             .map(Integer::parseInt)
                                             .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

        System.out.println(resourceCapacity);
        // Process successors
        for (int job = 18; job < 18 + numberJob; job++) {
            jobNumSuccessors.add(Integer.parseInt(data.get(job)[2]));
            List<Integer> successors = new ArrayList<>();
            for (int i = 3; i < data.get(job).length; i++) {
                successors.add(Integer.parseInt(data.get(job)[i]));
            }
            jobSuccessors.add(successors);
        }

        // Process predecessors
        for (int i = 0; i < numberJob; i++) {
            jobPredecessors.add(new ArrayList<>());
        }
        for (int job = 0; job < numberJob; job++) {
            for (int successor : jobSuccessors.get(job)) {
                jobPredecessors.get(successor - 1).add(job + 1);
            }
        }

        // Process duration and resources
        int row = 54;
        for (int job = 0; job < numberJob; job++) {
            jobDuration.add(Integer.parseInt(data.get(row)[2]));
            List<Integer> resources = new ArrayList<>();
            for (int i = 3; i < 7; i++) {
                resources.add(Integer.parseInt(data.get(row)[i]));
            }
            jobResource.add(resources);
            row++;
        }

        String instanceName = file.substring(file.lastIndexOf('/') + 1, file.lastIndexOf('.'));

        return new JobDataInstance(instanceName, numberJob, horizon, jobNumSuccessors, jobSuccessors,
                         jobPredecessors, jobDuration, jobResource, resourceCapacity);
    }
    
    public DataEvaluationInstance getDataFromResultFile(String filename) throws Exception {
        List<String> parameters = new ArrayList<>();
        List<String> instances = new ArrayList<>();
        List<String> modelTypes = new ArrayList<>();
        List<Integer> hMakespanList = new ArrayList<>();
        List<Integer> noHMakespanList = new ArrayList<>();
        List<Integer> hUBList = new ArrayList<>();
        List<Integer> hLBList = new ArrayList<>();
        List<Integer> optimalMakespanList = new ArrayList<>();
        List<Double> hTimeList = new ArrayList<>();
        List<Double> noHTimeList = new ArrayList<>();
        List<Double> timeDiffList = new ArrayList<>();
        List<Integer> heuristicMakespanList = new ArrayList<>();
        List<Boolean> timeLimitReachedList = new ArrayList<>();
        List<Boolean> errorList = new ArrayList<>();
        List<String> heuristicsList = new ArrayList<>();
        
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filename)))) {
            String line;
            String currentParameter = "";
            String currentInstance = "";
            
            while ((line = br.readLine()) != null) {
                line = line.trim();
                
                // Skip empty lines and header content
                boolean skipHeader = true;
                if (skipHeader && (line.isEmpty() || !line.startsWith("-"))) {
                    continue;
                } else if (skipHeader && line.startsWith("-")) {
                    skipHeader = false;
                    continue;
                }
                
                // Parse data lines
                String[] parts = line.split("\\s+"); // Split on any whitespace (spaces or tabs)
                if (parts.length >= 14) { // Ensure we have enough columns
                    // Handle parameter and instance (may be empty for continuation rows)
                    String parameter = parts[0].trim();
                    String instance = parts[1].trim();
                    String modelType = parts[2].trim();
                    
                    // If the first field looks like a model type, this is a continuation row
                    if (isModelType(parameter) && instance.matches("\\d+") && modelType.matches("\\d+")) {
                        // This is a continuation row where parameter column contains model type
                        modelType = parameter;
                        // Shift all values to the left
                        parameters.add(currentParameter);
                        instances.add(currentInstance);
                        modelTypes.add(modelType);
                        
                        // Parse shifted values
                        hMakespanList.add(parseIntOrDefault(instance, -1)); // instance is actually H_M_Makespan
                        noHMakespanList.add(parseIntOrDefault(parts[2].trim(), -1));
                        hUBList.add(parseIntOrDefault(parts[3].trim(), -1));
                        hLBList.add(parseIntOrDefault(parts[4].trim(), -1));
                        optimalMakespanList.add(parseIntOrDefault(parts[5].trim(), -1));
                        hTimeList.add(parseDoubleOrDefault(parts[6].trim(), -1.0));
                        noHTimeList.add(parseDoubleOrDefault(parts[7].trim(), -1.0));
                        timeDiffList.add(parseDoubleOrDefault(parts[8].trim(), -1.0));
                        heuristicMakespanList.add(parseIntOrDefault(parts[9].trim(), -1));
                        timeLimitReachedList.add(parseBooleanOrDefault(parts[10].trim(), false));
                        errorList.add(parseBooleanOrDefault(parts[11].trim(), false));
                        
                        // Handle heuristics (may span multiple columns)
                        StringBuilder heuristics = new StringBuilder();
                        for (int i = 12; i < parts.length; i++) {
                            if (i > 12) heuristics.append(" ");
                            heuristics.append(parts[i].trim());
                        }
                        heuristicsList.add(heuristics.toString());
                    } else {
                        // Normal row with parameter and instance
                        if (!parameter.isEmpty() && !instance.isEmpty()) {
                            currentParameter = parameter;
                            currentInstance = instance;
                        }
                        
                        parameters.add(currentParameter);
                        instances.add(currentInstance);
                        modelTypes.add(modelType);
                        
                        // Parse numeric values with error handling
                        hMakespanList.add(parseIntOrDefault(parts[3].trim(), -1));
                        noHMakespanList.add(parseIntOrDefault(parts[4].trim(), -1));
                        hUBList.add(parseIntOrDefault(parts[5].trim(), -1));
                        hLBList.add(parseIntOrDefault(parts[6].trim(), -1));
                        optimalMakespanList.add(parseIntOrDefault(parts[7].trim(), -1));
                        hTimeList.add(parseDoubleOrDefault(parts[8].trim(), -1.0));
                        noHTimeList.add(parseDoubleOrDefault(parts[9].trim(), -1.0));
                        timeDiffList.add(parseDoubleOrDefault(parts[10].trim(), -1.0));
                        heuristicMakespanList.add(parseIntOrDefault(parts[11].trim(), -1));
                        timeLimitReachedList.add(parseBooleanOrDefault(parts[12].trim(), false));
                        errorList.add(parseBooleanOrDefault(parts[13].trim(), false));
                        
                        // Handle heuristics (may span multiple columns)
                        StringBuilder heuristics = new StringBuilder();
                        for (int i = 14; i < parts.length; i++) {
                            if (i > 14) heuristics.append(" ");
                            heuristics.append(parts[i].trim());
                        }
                        heuristicsList.add(heuristics.toString());
                    }
                }
            }
        }
        
        // Convert lists to arrays
        String[] parameterArray = parameters.toArray(new String[0]);
        String[] instanceArray = instances.toArray(new String[0]);
        String[] modelTypeArray = modelTypes.toArray(new String[0]);
        int[] hMakespanArray = hMakespanList.stream().mapToInt(Integer::intValue).toArray();
        int[] noHMakespanArray = noHMakespanList.stream().mapToInt(Integer::intValue).toArray();
        int[] hUBArray = hUBList.stream().mapToInt(Integer::intValue).toArray();
        int[] hLBArray = hLBList.stream().mapToInt(Integer::intValue).toArray();
        int[] optimalMakespanArray = optimalMakespanList.stream().mapToInt(Integer::intValue).toArray();
        double[] hTimeArray = hTimeList.stream().mapToDouble(Double::doubleValue).toArray();
        double[] noHTimeArray = noHTimeList.stream().mapToDouble(Double::doubleValue).toArray();
        double[] timeDiffArray = timeDiffList.stream().mapToDouble(Double::doubleValue).toArray();
        int[] heuristicMakespanArray = heuristicMakespanList.stream().mapToInt(Integer::intValue).toArray();
        boolean[] timeLimitReachedArray = new boolean[timeLimitReachedList.size()];
        for (int i = 0; i < timeLimitReachedList.size(); i++) {
            timeLimitReachedArray[i] = timeLimitReachedList.get(i);
        }
        
        boolean[] errorArray = new boolean[errorList.size()];
        for (int i = 0; i < errorList.size(); i++) {
            errorArray[i] = errorList.get(i);
        }
        String[] heuristicsArray = heuristicsList.toArray(new String[0]);
        
        return new DataEvaluationInstance(parameterArray, instanceArray, modelTypeArray,
                hMakespanArray, noHMakespanArray, hUBArray, hLBArray, optimalMakespanArray,
                hTimeArray, noHTimeArray, timeDiffArray, heuristicMakespanArray,
                timeLimitReachedArray, errorArray, heuristicsArray);
    }
    
    private boolean isModelType(String value) {
        try {
            ModelType.valueOf(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    private int parseIntOrDefault(String value, int defaultValue) {
        if (value == null || value.equals("N/A") || value.equals("INFEASIBLE") || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    private double parseDoubleOrDefault(String value, double defaultValue) {
        if (value == null || value.equals("N/A") || value.equals("INFEASIBLE") || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    private boolean parseBooleanOrDefault(String value, boolean defaultValue) {
        if (value == null || value.equals("N/A") || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Boolean.parseBoolean(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }
    
    public Map<String, Integer> loadOptimalValues() {
        Map<String, Integer> optimalValues = new HashMap<>();
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
        return optimalValues;
    }
    
    public Map<String, ExistingResultData> loadExistingResults(String filename) throws Exception {
        Map<String, ExistingResultData> existingResults = new HashMap<>();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename)))) {
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
                        ExistingResultData data = parseExistingResultLine(parts);
                        existingResults.put(key, data);
                    }
                }
            }
        }
        return existingResults;
    }
    
    private ExistingResultData parseExistingResultLine(String[] parts) {
        ExistingResultData data = new ExistingResultData();
        
        if (parts.length > 3) data.hMakespan = parseStringValue(parts[3]);
        if (parts.length > 4) data.noHMakespan = parseStringValue(parts[4]);
        if (parts.length > 5) data.hUB = parseStringValue(parts[5]);
        if (parts.length > 6) data.hLB = parseStringValue(parts[6]);
        // Skip optimalMakespan as it's always recalculated
        if (parts.length > 8) data.hTime = parseStringValue(parts[8]);
        if (parts.length > 9) data.noHTime = parseStringValue(parts[9]);
        // Skip timeDiff as it's always recalculated
        if (parts.length > 11) data.heuristicMakespan = parseStringValue(parts[11]);
        if (parts.length > 12) data.timeLimitReached = parseStringValue(parts[12]);
        if (parts.length > 13) data.error = parseStringValue(parts[13]);
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
    
    private String parseStringValue(String value) {
        return "N/A".equals(value) ? null : value;
    }
    
    // Helper class to store existing result data
    public static class ExistingResultData {
        public String hMakespan;
        public String noHMakespan;
        public String hUB;
        public String hLB;
        public String hTime;
        public String noHTime;
        public String heuristicMakespan;
        public String timeLimitReached;
        public String error;
        public String heuristics;
    }
}