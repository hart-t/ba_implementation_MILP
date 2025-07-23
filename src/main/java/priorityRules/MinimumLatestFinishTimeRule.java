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
    
}