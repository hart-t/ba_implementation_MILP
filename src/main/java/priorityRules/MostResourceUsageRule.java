package priorityRules;

import io.JobDataInstance;
import java.util.*;
import interfaces.PriorityRuleInterface;

public class MostResourceUsageRule implements PriorityRuleInterface {
    @Override
    public Comparator<Integer> getComparator(JobDataInstance data) {
        return Comparator.comparingInt(i -> 
            -data.jobResource.get(i).stream().mapToInt(Integer::intValue).sum());
    }

    @Override
    public List<Integer> getSampledList(JobDataInstance data, List<Integer> eligibleActivities) {
        if (eligibleActivities.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Calculate resource usage for all eligible activities
        Map<Integer, Integer> resourceUsage = new HashMap<>();
        int maxResourceUsage = 0;
        int minResourceUsage = Integer.MAX_VALUE;
        
        for (int job : eligibleActivities) {
            int totalResourceUsage = data.jobResource.get(job).stream()
                .mapToInt(Integer::intValue).sum();
            resourceUsage.put(job, totalResourceUsage);
            maxResourceUsage = Math.max(maxResourceUsage, totalResourceUsage);
            minResourceUsage = Math.min(minResourceUsage, totalResourceUsage);
        }
        
        List<Integer> result = new ArrayList<>();
        List<Integer> remaining = new ArrayList<>(eligibleActivities);
        Random random = new Random();
        
        while (!remaining.isEmpty()) {
            boolean jobSelectedThisIteration = false;
            
            for (int i = 0; i < remaining.size(); i++) {
                int job = remaining.get(i);
                int usage = resourceUsage.get(job);
                
                // Calculate probability: (usage - minResourceUsage + 1) / (maxResourceUsage - minResourceUsage + 1)
                // Jobs with higher resource usage get higher probability
                double probability;
                if (maxResourceUsage == minResourceUsage) {
                    probability = 1.0 / remaining.size(); // Equal probability if all have same usage
                } else {
                    probability = (double)(usage - minResourceUsage + 1) / (maxResourceUsage - minResourceUsage + 1);
                }
                
                if (random.nextDouble() < probability) {
                    result.add(job);
                    remaining.remove(i);
                    jobSelectedThisIteration = true;
                    break;
                }
            }
            
            // Fallback: if no job was selected, select the one with highest resource usage
            if (!jobSelectedThisIteration) {
                int bestJob = remaining.stream()
                    .max(Comparator.comparingInt(resourceUsage::get))
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
            
            // Find the maximum resource usage among remaining activities (best choice for this rule)
            int maxResourceUsage = remaining.stream()
                .mapToInt(job -> data.jobResource.get(job).stream().mapToInt(Integer::intValue).sum())
                .max()
                .orElse(0);
            
            // Calculate regret for each activity (difference from maximum)
            for (int job : remaining) {
                int resourceUsage = data.jobResource.get(job).stream().mapToInt(Integer::intValue).sum();
                double regret = maxResourceUsage - resourceUsage;
                regrets.add(regret);
                totalRegret += regret;
            }
            
            // If all activities have the same resource usage (totalRegret = 0), select randomly
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