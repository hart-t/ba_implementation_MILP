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
import static enums.WarmstartStrategy.*;

public class TestAllModelInstanceCombinations {
    
    public static void main(String[] args) {

        List<Result> results = new ArrayList<>();

        File[] files = new File("/path/to/benchmarkSets").listFiles();
        assert files != null;
        
        // Limit to just the first 2 files for testing
        // files = Arrays.copyOf(files, Math.min(2, files.length));
        
        // Sort files by numeric parameter and instance values
        Arrays.sort(files, (f1, f2) -> {
            String name1 = f1.getName();
            String name2 = f2.getName();
            
            // Extract parameter and instance numbers from filenames like "j301_1.sm"
            int[] nums1 = extractNumbers(name1);
            int[] nums2 = extractNumbers(name2);
            
            // First compare by parameter, then by instance
            if (nums1[0] != nums2[0]) {
                return Integer.compare(nums1[0], nums2[0]);
            }
            return Integer.compare(nums1[1], nums2[1]);
        });

        // Configure all heuristics to test
        // Rule-PriorityStrategy-SamplingMethod_NumberOfSchedules
        List<String> heuristicConfigs = Arrays.asList(
                 "SSGS-SPT-BRS_250",        // Serial SGS with Shortest Processing Time
                "SSGS-GRPW-BRS_250",      // Serial SGS with Greatest Rank Positional Weight
                "SSGS-MRU-BRS_250",       // Serial SGS with Most Resource Usage
                "SSGS-RSM-BRS_250",       // Serial SGS with Resource Scheduling Method
                "SSGS-MTS-BRS_250",       // Serial SGS with Most Total Successors
                "SSGS-MLST-BRS_250",      // Serial SGS with Minimum Latest Start Time
                "SSGS-MLFT-BRS_250",      // Serial SGS with Minimum Latest Finish Time
                "SSGS-MJS-BRS_250",      // Serial SGS with Minimum Job Slack
                "GA-SPT-NS"
            );
            
        // Configure models using simple string codes
        List<String> modelConfigs = Arrays.asList(
                "FCT-300",      // Flow-Based Continuous Time Model
                "DT-300",           // Discrete Time Model
            "OOE-300",           // On-Off Event Based Model
            "IEE-300",         // Interval Event Based Model
            "SEQ-300"            // Sequencing Model
        );

        FileWriter fileWriter = new FileWriter();

        for (File file : files) {
            String filename = "/home/tobsi/university/kit/benchmarkSets/" + file.getName();
            
            try {
                FileReader fileReader = new FileReader();
                JobDataInstance data = fileReader.dataRead(filename);
                
                System.out.println("=== Testing file: " + file.getName() + " ===");
                
                // Create a separate results list for this file's results
                List<Result> fileResults = new ArrayList<>();
                
                // Test each model with heuristics (VS)
                for (String modelConfig : modelConfigs) {
                    System.out.println("--- Model: " + modelConfig + " (with heuristics) ---");
                    
                    IntegratedApproach integratedApproach = new IntegratedApproach(heuristicConfigs, modelConfig, VS);
                    fileResults.add(integratedApproach.solve(data));
                    // dont store results cause the hetzner server memory leaks and the parser into csv file approach is better
                    fileResults = new ArrayList<>();
                }

                 
                // Test each model with heuristics (VH)
                for (String modelConfig : modelConfigs) {
                    System.out.println("--- Model: " + modelConfig + " (with heuristics) ---");
                    
                    IntegratedApproach integratedApproach = new IntegratedApproach(heuristicConfigs, modelConfig, VH);
                    fileResults.add(integratedApproach.solve(data));
                    // dont store results cause the hetzner server memory leaks and the parser into csv file approach is better
                    fileResults = new ArrayList<>();
                }
                
                // Test each model without heuristics
                for (String modelConfig : modelConfigs) {
                    System.out.println("--- Model: " + modelConfig + " (without heuristics) ---");
                    
                    IntegratedApproach integratedApproach = new IntegratedApproach(Arrays.asList(), modelConfig, STD); // Empty heuristics list
                    fileResults.add(integratedApproach.solve(data));
                    // dont store results cause the hetzner server memory leaks and the parser into csv file approach is better
                    fileResults = new ArrayList<>();
                }
                
                // Add to overall results list
                //results.addAll(fileResults);
                
                // Write results to file immediately after processing this instance
                try {
                    fileWriter.writeResults(".", "test.txt", results);
                    System.out.println("Results updated in test.txt (total results: " + results.size() + ")");
                } catch (Exception e) {
                    System.err.println("Error writing results after processing " + file.getName() + ": " + e.getMessage());
                }
                
            } catch (Exception e) {
                System.err.println("Error processing file " + file.getName() + ": " + e.getMessage());
                // Continue with next file instead of returning
                continue;
            }
        }
        
        System.out.println("=== All files processed ===");
        System.out.println("Final total results: " + results.size());
    }
    
    private static int[] extractNumbers(String filename) {
        try {
            // Extract from "j301_1.sm" -> parameter=1, instance=1
            if (filename.startsWith("j30") && filename.contains("_")) {
                String withoutPrefix = filename.substring(3); // Remove "j30"
                String withoutExtension = withoutPrefix.split("\\.")[0]; // Remove ".sm"
                String[] parts = withoutExtension.split("_");
                if (parts.length >= 2) {
                    int parameter = Integer.parseInt(parts[0]);
                    int instance = Integer.parseInt(parts[1]);
                    return new int[]{parameter, instance};
                }
            }
        } catch (NumberFormatException e) {
            // Fallback to alphabetical if parsing fails
        }
        return new int[]{0, 0}; // Default values for unparseable filenames
    }
}
