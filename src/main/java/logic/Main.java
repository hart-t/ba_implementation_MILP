package logic;

import io.FileReader;
import java.util.Arrays;
import java.util.List;

import io.JobDataInstance;

public class Main {
    public static void main(String[] args) {
        String filename = "/home/tobsi/university/kit/benchmarkSets/j303_5.sm";

        try {
            FileReader fileReader = new FileReader();
            JobDataInstance data = fileReader.dataRead(filename);
            
            // Configure heuristics using simple string codes
            List<String> heuristicConfigs = Arrays.asList(
                "SGS-SPT",  // Serial SGS with Shortest Processing Time
                "SGS-RPW"   // Serial SGS with Rank Positional Weight
                // "SGS-MRU"   // Serial SGS with Most Resource Usage
            );
            
            // Configure models using simple string codes
            List<String> modelConfigs = Arrays.asList(
                "FLOW"      // Flow-Based Continuous Time Model
                // "DISC",     // Discrete Time Model
                // "EVENT"     // On-Off Event Based Model
            );

            // Solve with each model
            for (String modelConfig : modelConfigs) {
                IntegratedApproach integratedApproach = new IntegratedApproach(heuristicConfigs, modelConfig);
                integratedApproach.solve(data).printResult();
            }
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            return;
        }
    }
}