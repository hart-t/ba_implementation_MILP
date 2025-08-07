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
                    break;
                }
            }
            
            // Fallback: if no job was selected, select the one with highest resource usage
            if (remaining.size() == eligibleActivities.size() - result.size() && 
                remaining.size() == eligibleActivities.size()) {
                int bestJob = remaining.stream()
                    .max(Comparator.comparingInt(resourceUsage::get))
                    .orElse(remaining.get(0));
                remaining.remove((Integer) bestJob);
                result.add(bestJob);
            }
        }
        
        return result;
    }
}