package io;

import java.io.BufferedWriter;
import java.util.List;
import java.util.Map;

public class FileWriter {
    
    private FileReader fileReader;
    
    public FileWriter() {
        this.fileReader = new FileReader();
    }
    
    /**
     * Writes results to a file. If the file doesn't exist, creates a new file with appropriate format.
     * For parameter=1, instance=1, creates a new file with header.
     * For other instances, appends to existing file.
     * 
     * @param directory The directory where the file should be written
     * @param filename The name of the file
     * @param results The list of results to write
     * @throws Exception if there's an error writing to the file
     */
    public void writeResults(String directory, String filename, List<Result> results) throws Exception {
        if (results == null || results.isEmpty()) {
            throw new IllegalArgumentException("Results list cannot be null or empty");
        }
        
        String filepath = directory + "/" + filename;
        Map<String, Integer> optimalValues = fileReader.loadOptimalValues();
        
        // Format the new results - no reading of existing file
        ResultFormatter formatter = new ResultFormatter(optimalValues);
        List<String> lines = formatter.formatResults(results, null);  // Pass null for existingResults
        
        // Determine whether to append or overwrite
        boolean shouldAppend = true;
        Result firstResult = results.get(0);
        String[] instanceParts = extractInstanceInfo(firstResult.instanceName);
        
        if (instanceParts.length >= 2 && !instanceParts[0].isEmpty()) {
            try {
                int parameter = Integer.parseInt(instanceParts[0]);
                int instance = Integer.parseInt(instanceParts[1]);
                
                // Overwrite file if this is parameter 1, instance 1
                if (parameter == 1 && instance == 1) {
                    shouldAppend = false;
                }
            } catch (NumberFormatException e) {
                // If parsing fails, default to append to be safe
                System.err.println("Warning: Could not parse parameter/instance from " + 
                                   firstResult.instanceName + ", defaulting to append mode");
            }
        }
        
        // Write to file
        try (java.io.FileWriter writer = new java.io.FileWriter(filepath, shouldAppend);
             BufferedWriter bw = new BufferedWriter(writer)) {
            for (String line : lines) {
                bw.write(line);
                bw.newLine();
            }
        }
    }
    
    /**
     * Extracts parameter and instance numbers from an instance name.
     * Expected format: j30{parameter}_{instance}.sm
     * 
     * @param instanceName The instance name to parse
     * @return Array with [parameter, instance] as strings, or empty strings if parsing fails
     */
    private String[] extractInstanceInfo(String instanceName) {
        if (instanceName == null || !instanceName.startsWith("j30")) {
            return new String[]{"", ""};
        }
        
        try {
            String withoutPrefix = instanceName.substring(3);
            String withoutExtension = withoutPrefix.split("\\.")[0];
            String[] parts = withoutExtension.split("_");
            
            if (parts.length >= 2) {
                return new String[]{parts[0], parts[1]};
            }
        } catch (Exception e) {
            System.err.println("Warning: Error parsing instance name: " + instanceName);
        }
        
        return new String[]{"", ""};
    }
}