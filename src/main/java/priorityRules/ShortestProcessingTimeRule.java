package priorityRules;

import io.JobDataInstance;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

import interfaces.PriorityRuleInterface;

public class ShortestProcessingTimeRule implements PriorityRuleInterface {
    @Override
    public Comparator<Integer> getComparator(JobDataInstance data) {
        return Comparator.comparingInt(i -> data.jobDuration.get(i));
    }

    @Override
    public List<Integer> getSampledList(JobDataInstance data, List<Integer> eligibleActivities) {
        if (eligibleActivities.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Calculate total processing time of all eligible activities
        int totalProcessingTime = eligibleActivities.stream()
            .mapToInt(job -> data.jobDuration.get(job))
            .sum();
        
        List<Integer> result = new ArrayList<>();
        List<Integer> remaining = new ArrayList<>(eligibleActivities);
        Random random = new Random();
        
        int remainingTotalTime = totalProcessingTime;
        
        while (!remaining.isEmpty()) {
            boolean jobSelectedThisIteration = false;
            
            for (int i = 0; i < remaining.size(); i++) {
                int job = remaining.get(i);
                int processingTime = data.jobDuration.get(job);
                
                // Calculate probability: 1 - (processingTime / remainingTotalTime)
                double probability = 1.0 - ((double) processingTime / remainingTotalTime);
                
                if (random.nextDouble() < probability) {
                    result.add(job);
                    remaining.remove(i);
                    remainingTotalTime -= processingTime;
                    jobSelectedThisIteration = true;
                    break;
                }
            }
            
            // Fallback: if no job was selected in this iteration, select the shortest one
            if (!jobSelectedThisIteration) {
                int bestJobIndex = 0;
                int bestJobDuration = data.jobDuration.get(remaining.get(0));
                for (int i = 1; i < remaining.size(); i++) {
                    int jobDuration = data.jobDuration.get(remaining.get(i));
                    if (jobDuration < bestJobDuration) {
                        bestJobDuration = jobDuration;
                        bestJobIndex = i;
                    }
                }
                int job = remaining.remove(bestJobIndex);
                result.add(job);
                remainingTotalTime -= data.jobDuration.get(job);
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
            
            // Find the minimum processing time among remaining activities
            int minProcessingTime = remaining.stream()
                .mapToInt(job -> data.jobDuration.get(job))
                .min()
                .orElse(0);
            
            // Calculate regret for each activity (difference from minimum)
            for (int job : remaining) {
                double regret = data.jobDuration.get(job) - minProcessingTime;
                regrets.add(regret);
                totalRegret += regret;
            }
            
            // If all activities have the same processing time (totalRegret = 0), select randomly
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