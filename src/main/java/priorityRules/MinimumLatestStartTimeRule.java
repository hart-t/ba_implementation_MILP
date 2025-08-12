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
            boolean jobSelectedThisIteration = false;
            
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
                    jobSelectedThisIteration = true;
                    break;
                }
            }
            
            // Fallback: if no job was selected, select the one with minimum LST
            if (!jobSelectedThisIteration) {
                int bestJob = remaining.stream()
                    .min(Comparator.comparingInt(lstValues::get))
                    .orElse(remaining.get(0));
                remaining.remove((Integer) bestJob);
                result.add(bestJob);
            }
        }
        
        return result;
    }

    public List<Integer> getRegretBasedSampledList(JobDataInstance data, List<Integer> eligibleActivities) {
        if (eligibleActivities.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<Integer> result = new ArrayList<>();
        List<Integer> remaining = new ArrayList<>(eligibleActivities);
        Random random = new Random();
        
        while (!remaining.isEmpty()) {
            // Calculate regret for each remaining activity
            List<Double> regrets = new ArrayList<>();
            double totalRegret = 0.0;
            
            // Calculate latest start times for remaining activities
            int[] latestStartTimes = DAGLongestPath.generateEarliestAndLatestStartTimes
                    (data.jobPredecessors, data.jobDuration, data.horizon)[1];
            
            // Find the minimum LST among remaining activities (best choice for this rule)
            int minLst = remaining.stream()
                .mapToInt(job -> latestStartTimes[job])
                .min()
                .orElse(Integer.MAX_VALUE);
            
            // Calculate regret for each activity (difference from minimum)
            for (int job : remaining) {
                double regret = latestStartTimes[job] - minLst;
                regrets.add(regret);
                totalRegret += regret;
            }
            
            // If all activities have the same LST (totalRegret = 0), select randomly
            if (totalRegret == 0.0) {
                int selectedIndex = random.nextInt(remaining.size());
                result.add(remaining.remove(selectedIndex));
                continue;
            }
            
            // Calculate selection probabilities based on inverse regret
            // Activities with lower regret (closer to minimum) have higher probability
            List<Double> probabilities = new ArrayList<>();
            for (int i = 0; i < remaining.size(); i++) {
                // Inverse regret: higher probability for lower regret
                double inverseRegret = totalRegret - regrets.get(i);
                probabilities.add(inverseRegret);
            }
            
            // Normalize probabilities
            double totalInverseRegret = probabilities.stream().mapToDouble(Double::doubleValue).sum();
            for (int i = 0; i < probabilities.size(); i++) {
                probabilities.set(i, probabilities.get(i) / totalInverseRegret);
            }
            
            // Select activity based on probabilities
            double randomValue = random.nextDouble();
            double cumulativeProbability = 0.0;
            int selectedIndex = 0;
            
            for (int i = 0; i < probabilities.size(); i++) {
                cumulativeProbability += probabilities.get(i);
                if (randomValue <= cumulativeProbability) {
                    selectedIndex = i;
                    break;
                }
            }
            
            result.add(remaining.remove(selectedIndex));
        }
        
        return result;
    }
}