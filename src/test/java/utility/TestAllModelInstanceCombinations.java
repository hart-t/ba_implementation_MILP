package utility;

import io.FileReader;
import io.JobDataInstance;
import logic.IntegratedApproach;
import io.Result;
import io.FileWriter;

import java.util.Arrays;
import java.util.List;
import java.io.File;
import java.util.ArrayList;

public class TestAllModelInstanceCombinations {
    
    public static void main(String[] args) {

        List<Result> results = new ArrayList<>();

        File[] files = new File("/home/tobsi/university/kit/benchmarkSets2").listFiles();
        assert files != null;
        
        // Sort files alphabetically
        Arrays.sort(files, (f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));

        // Configure all heuristics to test
        List<String> heuristicConfigs = Arrays.asList(
                "SSGS-SPT",        // Serial SGS with Shortest Processing Time
                "SSGS-GRPW",      // Serial SGS with Greatest Rank Positional Weight
                "SSGS-MRU",       // Serial SGS with Most Resource Usage
                "SSGS-RSM",       // Serial SGS with Resource Scheduling Method
                "SSGS-MTS",       // Serial SGS with Most Total Successors
                "SSGS-MLST",      // Serial SGS with Minimum Latest Start Time
                "SSGS-MLFT",      // Serial SGS with Minimum Latest Finish Time
                "SSGS-MJS"       // Serial SGS with Minimum Job Slack
        );
        
        // Configure all models to test
        List<String> modelConfigs = Arrays.asList(
            "FLOW",     // Flow-Based Continuous Time Model
            "DISC",     // Discrete Time Model
            "EVENT"     // On-Off Event Based Model
        );

        for (File file : files) {
            String filename = "/home/tobsi/university/kit/benchmarkSets/" + file.getName();
            
            try {
                FileReader fileReader = new FileReader();
                JobDataInstance data = fileReader.dataRead(filename);
                
                System.out.println("\n=== Testing file: " + file.getName() + " ===");
                
                // Test each model
                for (String modelConfig : modelConfigs) {
                    System.out.println("\n--- Model: " + modelConfig + " ---");
                    
                    IntegratedApproach integratedApproach = new IntegratedApproach(heuristicConfigs, modelConfig);
                    results.add(integratedApproach.solve(data));
                }
                
            } catch (Exception e) {
                System.err.println("Error processing file " + file.getName() + ": " + e.getMessage());
                // Continue with next file instead of returning
                continue;
            }
        }
        // Write all results to a file
        FileWriter fileWriter = new FileWriter();
        try {
            fileWriter.writeResults("/home/tobsi/university/kit/results", "all_results.txt", results);
            System.out.println("Results written to /home/tobsi/university/kit/results/all_results.txt");
        } catch (Exception e) {
            System.err.println("Error writing results: " + e.getMessage());
        }
    }
}
