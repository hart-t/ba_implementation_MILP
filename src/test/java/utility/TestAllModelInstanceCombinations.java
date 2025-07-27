package utility;

import io.FileReader;
import io.JobDataInstance;
import logic.IntegratedApproach;

import java.util.Arrays;
import java.util.List;
import java.io.File;

public class TestAllModelInstanceCombinations {
    
    public static void main(String[] args) {

        File[] files = new File("/home/tobsi/university/kit/benchmarkSets").listFiles();
        assert files != null;

        // Configure all heuristics to test
        List<String> heuristicConfigs = Arrays.asList(
            "SGS-SPT",  // Serial SGS with Shortest Processing Time
            "SGS-RPW",  // Serial SGS with Rank Positional Weight
            "SGS-MRU"   // Serial SGS with Most Resource Usage
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
                    integratedApproach.solve(data);
                }
                
            } catch (Exception e) {
                System.err.println("Error processing file " + file.getName() + ": " + e.getMessage());
                // Continue with next file instead of returning
                continue;
            }
        }
    }
}
