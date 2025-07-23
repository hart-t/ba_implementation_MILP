package priorityRules;

import io.JobDataInstance;
import java.util.*;
import interfaces.PriorityRuleInterface;

/*
 * Alvarez-Valdes and Tamarit 1993
 * TODO
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
                
                // Alternative: sum all successor RPWs for total remaining work
                // Uncomment below and comment above for sum-based approach
                /*
                int sumSuccessorRPW = 0;
                for (int successor : data.jobSuccessors.get(i)) {
                    int successorId = successor - 1;
                    sumSuccessorRPW += rpw.get(successorId);
                }
                currentRPW += sumSuccessorRPW;
                */
                
                currentRPW += maxSuccessorRPW;
                
                if (currentRPW > rpw.get(i)) {
                    rpw.put(i, currentRPW);
                    changed = true;
                }
            }
        }
        
        return rpw;
    }
}