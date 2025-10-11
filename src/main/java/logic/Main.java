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
 * 
 * 
 * 
 * 
 * TODO 14.08.2025
 * 
 * Callback done
 * time tracking of heuristics done
 * ba schreiben
 * letztes model
 * aws server und laufen lassen
 * 
 */

public class Main {
    public static void main(String[] args) {
        String filename = "/home/tobsi/university/kit/benchmarkSets/j305_5.sm";

        try {
            FileReader fileReader = new FileReader();
            JobDataInstance data = fileReader.dataRead(filename);
            
            /*
             * Configure heuristics using simple string codes
             * If multiple heuristics/priority rules are selected, start times will be generated for each 
             * heuristic and the schedule with the lowest makespan will be chosen.
             */
            List<String> heuristicConfigs = Arrays.asList(
                //"SSGS-SPT-RBRS_500"        // Serial SGS with Shortest Processing Time
                //"SSGS-GRPW-NS",      // Serial SGS with Greatest Rank Positional Weight
                //"SSGS-MRU-NS",       // Serial SGS with Most Resource Usage
                //"SSGS-RSM-NS",       // Serial SGS with Resource Scheduling Method
                //"SSGS-MTS-NS",       // Serial SGS with Most Total Successors
                //"SSGS-MLST-NS",      // Serial SGS with Minimum Latest Start Time
                //"SSGS-MLFT-NS",      // Serial SGS with Minimum Latest Finish Time
                //"SSGS-MJS-NS"       // Serial SGS with Minimum Job Slack
                //"GA-SPT-NS"
            );
            
            // Configure models using simple string codes
            List<String> modelConfigs = Arrays.asList(
                 //"FCT-30"      // Flow-Based Continuous Time Model
                 //"DT-30"     // Discrete Time Model
                //"OOE-30"     // On-Off Event Based Model
                 //"IEE-600"     // Interval Event Based Model
                "SEQ-30"     // Sequencing Model
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