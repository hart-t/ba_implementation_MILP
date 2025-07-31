package io;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

public class FileWriter {
    
    private ResultFormatter formatter;
    
    public FileWriter() {
        this.formatter = new ResultFormatter();
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
        
        // Format the results using the formatter
        List<String> lines = formatter.formatResults(results, file);
        
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