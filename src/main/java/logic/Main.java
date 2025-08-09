package logic;

import io.FileReader;
import java.util.Arrays;
import java.util.List;

import io.JobDataInstance;

/*
 * TODO
 * 
 * ich habe zwei heuristisch ermittelte schedules mit gleicher makespan aber unterschiedlichen startzeiten
 * wie vergleiche ich diese um die bessere zu finden?
 * 
 * 
 * https://www.sciencedirect.com/science/article/pii/S0377221724005162?via%3Dihub 4.0
 * sequencing model
 * 
 * 30 vars zu viel bei IEE vermutlich erste einfach nicht mitgezählt?
 * 
 */

public class Main {
    public static void main(String[] args) {
        String filename = "/home/tobsi/university/kit/benchmarkSets/j301_1.sm";

        try {
            FileReader fileReader = new FileReader();
            JobDataInstance data = fileReader.dataRead(filename);
            
            /*
             * Configure heuristics using simple string codes
             * If multiple heuristics/priority rules are selected, start times will be generated for each 
             * heuristic and the schedule with the lowest makespan will be chosen.
             */
            List<String> heuristicConfigs = Arrays.asList(
                "SSGS-SPT-BRS"        // Serial SGS with Shortest Processing Time
                //"SSGS-GRPW-NS",      // Serial SGS with Greatest Rank Positional Weight
                //"SSGS-MRU-NS",       // Serial SGS with Most Resource Usage
                //"SSGS-RSM-NS",       // Serial SGS with Resource Scheduling Method
                //"SSGS-MTS-NS",       // Serial SGS with Most Total Successors
                //"SSGS-MLST-NS",      // Serial SGS with Minimum Latest Start Time
                //"SSGS-MLFT-NS",      // Serial SGS with Minimum Latest Finish Time
                //"SSGS-MJS-NS"       // Serial SGS with Minimum Job Slack
            );
            
            // Configure models using simple string codes
            List<String> modelConfigs = Arrays.asList(
                 //"FLOW"      // Flow-Based Continuous Time Model
                 "DISC"     // Discrete Time Model
                 //"EVENT"     // On-Off Event Based Model
                 // "IEE"     // Interval Event Based Model
            );

            // Solve with each model
            for (String modelConfig : modelConfigs) {
                IntegratedApproach integratedApproach = new IntegratedApproach(heuristicConfigs, modelConfig);
                integratedApproach.solve(data);
            }
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            return;
        }
    }
}