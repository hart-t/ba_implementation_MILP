package priorityRules;

import io.JobDataInstance;
import java.util.*;
import interfaces.PriorityRuleInterface;
import static utility.DAGLongestPath.generateEarliestAndLatestStartTimes;

/*
 * Resource Scheduling Method (RSM) Rule
 * This rule was developed at the University of Illinois by Brand, Meyer and Shaffer,
 * who reported it most effective in a series of tests conducted on construction-industry
 * projects
 *
 *     https://www.jstor.org/stable/2629856?seq=5
 */

public class ResourceSchedulingMethodRule implements PriorityRuleInterface {
    @Override
    public Comparator<Integer> getComparator(JobDataInstance data) {
        int[][] earliestAndLatestStartTimes = generateEarliestAndLatestStartTimes
                (data.jobPredecessors, data.jobDuration, data.horizon);
        int[] earliestStartTimes = earliestAndLatestStartTimes[0];
        int[] latestStartTimes = earliestAndLatestStartTimes[1];
        
        return Comparator.comparingInt(i -> {
            int minDij = Integer.MAX_VALUE;
            int eftI = earliestStartTimes[i] + data.jobDuration.get(i); // Earliest Finish Time of activity i

            // Compare with all other jobs to find minimum d_ij
            for (int j = 0; j < data.numberJob; j++) {
                if (i != j) {
                    int lstJ = latestStartTimes[j]; // Late Start Time of activity j
                    int dij = Math.max(0, eftI - lstJ);
                    minDij = Math.min(minDij, dij);
                }
            }
            
            return minDij == Integer.MAX_VALUE ? 0 : minDij;
        });
    }

    @Override
    public List<Integer> getSampledList(JobDataInstance data, List<Integer> eligibleActivities) {
        if (eligibleActivities.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Pre-calculate RSM values and earliest/latest start times
        int[][] earliestAndLatestStartTimes = generateEarliestAndLatestStartTimes
                (data.jobPredecessors, data.jobDuration, data.horizon);
        int[] earliestStartTimes = earliestAndLatestStartTimes[0];
        int[] latestStartTimes = earliestAndLatestStartTimes[1];
        
        // Calculate RSM values for all eligible activities
        Map<Integer, Integer> rsmValues = new HashMap<>();
        int maxRsmValue = 0;
        
        for (int job : eligibleActivities) {
            int minDij = Integer.MAX_VALUE;
            int eftI = earliestStartTimes[job] + data.jobDuration.get(job);
            
            for (int j = 0; j < data.numberJob; j++) {
                if (job != j) {
                    int lstJ = latestStartTimes[j];
                    int dij = Math.max(0, eftI - lstJ);
                    minDij = Math.min(minDij, dij);
                }
            }
            
            int rsmValue = minDij == Integer.MAX_VALUE ? 0 : minDij;
            rsmValues.put(job, rsmValue);
            maxRsmValue = Math.max(maxRsmValue, rsmValue);
        }
        
        List<Integer> result = new ArrayList<>();
        List<Integer> remaining = new ArrayList<>(eligibleActivities);
        Random random = new Random();
        
        while (!remaining.isEmpty()) {
            for (int i = 0; i < remaining.size(); i++) {
                int job = remaining.get(i);
                int rsmValue = rsmValues.get(job);
                
                // Calculate probability: (maxRsmValue - rsmValue + 1) / (maxRsmValue + 1)
                // Jobs with lower RSM values get higher probability
                double probability = (double)(maxRsmValue - rsmValue + 1) / (maxRsmValue + 1);
                
                if (random.nextDouble() < probability) {
                    result.add(job);
                    remaining.remove(i);
                    break;
                }
            }
            
            // Fallback: if no job was selected, select the one with lowest RSM value
            if (remaining.size() == eligibleActivities.size() - result.size() && 
                remaining.size() == eligibleActivities.size()) {
                int bestJob = remaining.stream()
                    .min(Comparator.comparingInt(rsmValues::get))
                    .orElse(remaining.get(0));
                remaining.remove((Integer) bestJob);
                result.add(bestJob);
            }
        }
        
        return result;
    }
}