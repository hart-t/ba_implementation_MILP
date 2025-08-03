package io;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileWriter {
    
    private FileReader fileReader;
    
    public FileWriter() {
        this.fileReader = new FileReader();
    }
    
    /**
     * Writes results to a file. If the file doesn't exist, creates a new file with appropriate format.
     * If the file exists, merges the new results with existing data.
     * 
     * @param directory The directory where the file should be written
     * @param filename The name of the file
     * @param results The list of results to write
     * @throws Exception if there's an error writing to the file
     */
    public void writeResults(String directory, String filename, List<Result> results) throws Exception {
        // Create directory if it doesn't exist
        File dir = new File(directory);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        // Create full file path
        File file = new File(dir, filename);
        
        // Load optimal values
        Map<String, Integer> optimalValues = fileReader.loadOptimalValues();
        
        // Load existing results if file exists
        Map<String, FileReader.ExistingResultData> existingResults = new HashMap<>();
        if (file.exists()) {
            existingResults = fileReader.loadExistingResults(file.getAbsolutePath());
        }
        
        // Create formatter with optimal values
        ResultFormatter formatter = new ResultFormatter(optimalValues);
        
        // Format the results using the formatter
        List<String> lines = formatter.formatResults(results, existingResults);
        
        // Write the lines to the file
        writeLinesToFile(file, lines);
    }
    
    /**
     * Writes a list of strings to a file, overwriting any existing content.
     * 
     * @param file The file to write to
     * @param lines The lines to write
     * @throws Exception if there's an error writing to the file
     */
    private void writeLinesToFile(File file, List<String> lines) throws Exception {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, false)))) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        }
    }
    
    /**
     * Appends a list of strings to an existing file.
     * 
     * @param file The file to append to
     * @param lines The lines to append
     * @throws Exception if there's an error writing to the file
     */
    public void appendLinesToFile(File file, List<String> lines) throws Exception {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true)))) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        }
    }
}