package priorityRules;

import io.JobDataInstance;
import java.util.*;
import interfaces.PriorityRuleInterface;

/*
 * Alvarez-Valdes and Tamarit 1993
 * https://www.sciencedirect.com/science/article/pii/037722179400272X
 * https://www.sciencedirect.com/science/article/pii/0377221787902402
 */

public class GreatestRankPositionalWeightRule implements PriorityRuleInterface {
    @Override
    public Comparator<Integer> getComparator(JobDataInstance data) {
        Map<Integer, Integer> rankPositionalWeights = calculateRankPositionalWeights(data);
        // Sort by RPW (descending - jobs with greater RPW have higher priority)
        return Comparator.comparingInt((Integer i) -> -rankPositionalWeights.get(i))
                         .thenComparingInt(i -> i); // tie-breaker by job ID
    }
    
    private Map<Integer, Integer> calculateRankPositionalWeights(JobDataInstance data) {
        Map<Integer, Integer> rpw = new HashMap<>();
        
        // Initialize RPW for all jobs to their own duration
        for (int i = 0; i < data.numberJob; i++) {
            rpw.put(i, data.jobDuration.get(i));
        }
        
        // Calculate RPW using backward propagation through the precedence network
        boolean changed = true;
        while (changed) {
            changed = false;
            
            for (int i = 0; i < data.numberJob; i++) {
                int currentRPW = data.jobDuration.get(i);
                
                // Add the maximum RPW of all successors
                int maxSuccessorRPW = 0;
                for (int successor : data.jobSuccessors.get(i)) {
                    int successorId = successor - 1; // Convert to 0-based indexing
                    maxSuccessorRPW = Math.max(maxSuccessorRPW, rpw.get(successorId));
                }
                
                currentRPW += maxSuccessorRPW;
                
                if (currentRPW > rpw.get(i)) {
                    rpw.put(i, currentRPW);
                    changed = true;
                }
            }
        }
        
        return rpw;
    }

    @Override
    public List<Integer> getSampledList(JobDataInstance data, List<Integer> eligibleActivities) {
        if (eligibleActivities.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Calculate RPW values for all eligible activities
        Map<Integer, Integer> allRpwValues = calculateRankPositionalWeights(data);
        Map<Integer, Integer> eligibleRpwValues = new HashMap<>();
        int maxRpw = Integer.MIN_VALUE;
        int minRpw = Integer.MAX_VALUE;
        
        for (int job : eligibleActivities) {
            int rpw = allRpwValues.get(job);
            eligibleRpwValues.put(job, rpw);
            maxRpw = Math.max(maxRpw, rpw);
            minRpw = Math.min(minRpw, rpw);
        }
        
        List<Integer> result = new ArrayList<>();
        List<Integer> remaining = new ArrayList<>(eligibleActivities);
        Random random = new Random();
        
        while (!remaining.isEmpty()) {
            boolean jobSelectedThisIteration = false;
            
            for (int i = 0; i < remaining.size(); i++) {
                int job = remaining.get(i);
                int rpw = eligibleRpwValues.get(job);
                
                // Calculate probability: (rpw - minRpw + 1) / (maxRpw - minRpw + 1)
                // Jobs with higher RPW get higher probability
                double probability;
                if (maxRpw == minRpw) {
                    probability = 1.0 / remaining.size(); // Equal probability if all have same RPW
                } else {
                    probability = (double)(rpw - minRpw + 1) / (maxRpw - minRpw + 1);
                }
                
                if (random.nextDouble() < probability) {
                    result.add(job);
                    remaining.remove(i);
                    jobSelectedThisIteration = true;
                    break;
                }
            }
            
            // Fallback: if no job was selected, select the one with highest RPW
            if (!jobSelectedThisIteration) {
                int bestJob = remaining.stream()
                    .max(Comparator.comparingInt(eligibleRpwValues::get))
                    .orElse(remaining.get(0));
                remaining.remove((Integer) bestJob);
                result.add(bestJob);
            }
        }
        
        return result;
    }
}