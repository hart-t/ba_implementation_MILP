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
            
            // Calculate RPW values for remaining activities
            Map<Integer, Integer> allRpwValues = calculateRankPositionalWeights(data);
            
            // Find the maximum RPW among remaining activities (best choice for this rule)
            int maxRpw = remaining.stream()
                .mapToInt(job -> allRpwValues.get(job))
                .max()
                .orElse(0);
            
            // Calculate regret for each activity (difference from maximum)
            for (int job : remaining) {
                double regret = maxRpw - allRpwValues.get(job);
                regrets.add(regret);
                totalRegret += regret;
            }
            
            // If all activities have the same RPW (totalRegret = 0), select randomly
            if (totalRegret == 0.0) {
                int selectedIndex = random.nextInt(remaining.size());
                result.add(remaining.remove(selectedIndex));
                continue;
            }
            
            // Calculate selection probabilities based on inverse regret
            // Activities with lower regret (closer to maximum) have higher probability
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