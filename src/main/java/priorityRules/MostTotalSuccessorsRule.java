package priorityRules;

import io.JobDataInstance;
import java.util.*;
import interfaces.PriorityRuleInterface;

/*
 * Alvarez-Valdes and Tamarit 1993
 */

public class MostTotalSuccessorsRule implements PriorityRuleInterface {
    @Override
    public Comparator<Integer> getComparator(JobDataInstance data) {
        return Comparator.comparingInt(i -> 
            -data.jobSuccessors.get(i).size());
    }

    @Override
    public List<Integer> getSampledList(JobDataInstance data, List<Integer> eligibleActivities) {
        if (eligibleActivities.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Calculate successor counts for all eligible activities
        Map<Integer, Integer> successorCounts = new HashMap<>();
        int maxSuccessors = 0;
        int minSuccessors = Integer.MAX_VALUE;
        
        for (int job : eligibleActivities) {
            int successorCount = data.jobSuccessors.get(job).size();
            successorCounts.put(job, successorCount);
            maxSuccessors = Math.max(maxSuccessors, successorCount);
            minSuccessors = Math.min(minSuccessors, successorCount);
        }
        
        List<Integer> result = new ArrayList<>();
        List<Integer> remaining = new ArrayList<>(eligibleActivities);
        Random random = new Random();
        
        while (!remaining.isEmpty()) {
            boolean jobSelectedThisIteration = false;
            
            for (int i = 0; i < remaining.size(); i++) {
                int job = remaining.get(i);
                int successorCount = successorCounts.get(job);
                
                // Calculate probability: (successorCount - minSuccessors + 1) / (maxSuccessors - minSuccessors + 1)
                // Jobs with more successors get higher probability
                double probability;
                if (maxSuccessors == minSuccessors) {
                    probability = 1.0 / remaining.size(); // Equal probability if all have same count
                } else {
                    probability = (double)(successorCount - minSuccessors + 1) / (maxSuccessors - minSuccessors + 1);
                }
                
                if (random.nextDouble() < probability) {
                    result.add(job);
                    remaining.remove(i);
                    jobSelectedThisIteration = true;
                    break;
                }
            }
            
            // Fallback: if no job was selected, select the one with most successors
            if (!jobSelectedThisIteration) {
                int bestJob = remaining.stream()
                    .max(Comparator.comparingInt(successorCounts::get))
                    .orElse(remaining.get(0));
                remaining.remove((Integer) bestJob);
                result.add(bestJob);
            }
        }
        
        return result;
    }
}