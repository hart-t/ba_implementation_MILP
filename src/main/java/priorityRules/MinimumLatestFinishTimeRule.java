package priorityRules;

import io.JobDataInstance;
import utility.DAGLongestPath;

import java.util.*;
import interfaces.PriorityRuleInterface;

/*
 * Alvarez-Valdes and Tamarit 1993
 */

public class MinimumLatestFinishTimeRule implements PriorityRuleInterface {
    @Override
    public Comparator<Integer> getComparator(JobDataInstance data) {
        int[] latestStartTimes = DAGLongestPath.generateEarliestAndLatestStartTimes
                (data.jobPredecessors, data.jobDuration, data.horizon)[1];
        // Sort by latest finish time (ascending - jobs with earlier LFT have higher priority)
        return Comparator.comparingInt((Integer i) -> latestStartTimes[i] + data.jobDuration.get(i))
                         .thenComparingInt(i -> i); // tie-breaker by job ID
    }
 
    @Override
    public List<Integer> getSampledList(JobDataInstance data, List<Integer> eligibleActivities) {
        if (eligibleActivities.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Calculate latest finish times for all eligible activities
        int[] latestStartTimes = DAGLongestPath.generateEarliestAndLatestStartTimes
                (data.jobPredecessors, data.jobDuration, data.horizon)[1];
        
        Map<Integer, Integer> lftValues = new HashMap<>();
        int maxLft = Integer.MIN_VALUE;
        int minLft = Integer.MAX_VALUE;
        
        for (int job : eligibleActivities) {
            int lft = latestStartTimes[job] + data.jobDuration.get(job);
            lftValues.put(job, lft);
            maxLft = Math.max(maxLft, lft);
            minLft = Math.min(minLft, lft);
        }
        
        List<Integer> result = new ArrayList<>();
        List<Integer> remaining = new ArrayList<>(eligibleActivities);
        Random random = new Random();
        
        while (!remaining.isEmpty()) {
            boolean jobSelectedThisIteration = false;
            
            for (int i = 0; i < remaining.size(); i++) {
                int job = remaining.get(i);
                int lft = lftValues.get(job);
                
                // Calculate probability: (maxLft - lft + 1) / (maxLft - minLft + 1)
                // Jobs with smaller LFT (earlier deadline) get higher probability
                double probability;
                if (maxLft == minLft) {
                    probability = 1.0 / remaining.size(); // Equal probability if all have same LFT
                } else {
                    probability = (double)(maxLft - lft + 1) / (maxLft - minLft + 1);
                }
                
                if (random.nextDouble() < probability) {
                    result.add(job);
                    remaining.remove(i);
                    jobSelectedThisIteration = true;
                    break;
                }
            }
            
            // Fallback: if no job was selected, select the one with minimum LFT
            if (!jobSelectedThisIteration) {
                int bestJob = remaining.stream()
                    .min(Comparator.comparingInt(lftValues::get))
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
            
            // Find the minimum LFT among remaining activities (best choice for this rule)
            int minLft = remaining.stream()
                .mapToInt(job -> latestStartTimes[job] + data.jobDuration.get(job))
                .min()
                .orElse(Integer.MAX_VALUE);
            
            // Calculate regret for each activity (difference from minimum)
            for (int job : remaining) {
                int lft = latestStartTimes[job] + data.jobDuration.get(job);
                double regret = lft - minLft;
                regrets.add(regret);
                totalRegret += regret;
            }
            
            // If all activities have the same LFT (totalRegret = 0), select randomly
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