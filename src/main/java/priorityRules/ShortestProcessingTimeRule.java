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
            for (int i = 0; i < remaining.size(); i++) {
                int job = remaining.get(i);
                int processingTime = data.jobDuration.get(job);
                
                // Calculate probability: 1 - (processingTime / remainingTotalTime)
                double probability = 1.0 - ((double) processingTime / remainingTotalTime);
                
                if (random.nextDouble() < probability) {
                    result.add(job);
                    remaining.remove(i);
                    remainingTotalTime -= processingTime;
                    break;
                }
            }
            
            // Fallback: if no job was selected in this iteration, select the first one
            if (remaining.size() == eligibleActivities.size() - result.size() && 
                remaining.size() == eligibleActivities.size()) {
                int job = remaining.remove(0);
                result.add(job);
                remainingTotalTime -= data.jobDuration.get(job);
            }
        }
        
        return result;
    }
}