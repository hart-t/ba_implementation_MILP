package priorityRules;

import io.JobDataInstance;
import utility.DAGLongestPath;

import java.util.*;
import interfaces.PriorityRuleInterface;

/*
 * Alvarez-Valdes and Tamarit 1993
 */

public class MinimumLatestStartTimeRule implements PriorityRuleInterface {
    @Override
    public Comparator<Integer> getComparator(JobDataInstance data) {
        int[] latestStartTimes = DAGLongestPath.generateEarliestAndLatestStartTimes
                (data.jobPredecessors, data.jobDuration, data.horizon)[1];
        // Sort by latest start time (ascending - jobs with earlier LST have higher priority for MLST)
        return Comparator.comparingInt((Integer i) -> latestStartTimes[i])
                         .thenComparingInt(i -> i); // tie-breaker by job ID
    }
    
    @Override
    public List<Integer> getSampledList(JobDataInstance data, List<Integer> eligibleActivities) {
        if (eligibleActivities.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Calculate latest start times for all eligible activities
        int[] latestStartTimes = DAGLongestPath.generateEarliestAndLatestStartTimes
                (data.jobPredecessors, data.jobDuration, data.horizon)[1];
        
        Map<Integer, Integer> lstValues = new HashMap<>();
        int maxLst = Integer.MIN_VALUE;
        int minLst = Integer.MAX_VALUE;
        
        for (int job : eligibleActivities) {
            int lst = latestStartTimes[job];
            lstValues.put(job, lst);
            maxLst = Math.max(maxLst, lst);
            minLst = Math.min(minLst, lst);
        }
        
        List<Integer> result = new ArrayList<>();
        List<Integer> remaining = new ArrayList<>(eligibleActivities);
        Random random = new Random();
        
        while (!remaining.isEmpty()) {
            for (int i = 0; i < remaining.size(); i++) {
                int job = remaining.get(i);
                int lst = lstValues.get(job);
                
                // Calculate probability: (maxLst - lst + 1) / (maxLst - minLst + 1)
                // Jobs with smaller LST (earlier deadline) get higher probability
                double probability;
                if (maxLst == minLst) {
                    probability = 1.0 / remaining.size(); // Equal probability if all have same LST
                } else {
                    probability = (double)(maxLst - lst + 1) / (maxLst - minLst + 1);
                }
                
                if (random.nextDouble() < probability) {
                    result.add(job);
                    remaining.remove(i);
                    break;
                }
            }
            
            // Fallback: if no job was selected, select the one with minimum LST
            if (remaining.size() == eligibleActivities.size() - result.size() && 
                remaining.size() == eligibleActivities.size()) {
                int bestJob = remaining.stream()
                    .min(Comparator.comparingInt(lstValues::get))
                    .orElse(remaining.get(0));
                remaining.remove((Integer) bestJob);
                result.add(bestJob);
            }
        }
        
        return result;
    }
}