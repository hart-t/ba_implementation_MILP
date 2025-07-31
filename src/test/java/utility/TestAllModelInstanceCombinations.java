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

        File[] files = new File("/home/tobsi/university/kit/benchmarkSets").listFiles();
        assert files != null;
        
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


        FileWriter fileWriter = new FileWriter();

        for (File file : files) {
            String filename = "/home/tobsi/university/kit/benchmarkSets/" + file.getName();
            
            try {
                FileReader fileReader = new FileReader();
                JobDataInstance data = fileReader.dataRead(filename);
                
                System.out.println("=== Testing file: " + file.getName() + " ===");
                
                // Create a separate results list for this file's results
                List<Result> fileResults = new ArrayList<>();
                
                // Test each model
                for (String modelConfig : modelConfigs) {
                    System.out.println("--- Model: " + modelConfig + " ---");
                    
                    IntegratedApproach integratedApproach = new IntegratedApproach(heuristicConfigs, modelConfig);
                    fileResults.add(integratedApproach.solve(data));
                }

                // Write results for this file only
                fileWriter.writeResults("/home/tobsi/university/kit/results", "test.txt", fileResults);
                System.out.println("Results for " + file.getName() + " written to /home/tobsi/university/kit/results/test.txt");
                
                // Add to overall results list for potential future use
                results.addAll(fileResults);
                
            } catch (Exception e) {
                System.err.println("Error processing file " + file.getName() + ": " + e.getMessage());
                // Continue with next file instead of returning
                continue;
            }
        }
        
        System.out.println("=== All files processed ===");
        System.out.println("Total results collected: " + results.size());
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
