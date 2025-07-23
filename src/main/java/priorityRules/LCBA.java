package priorityRules;

import io.JobDataInstance;
import utility.DAGLongestPath;

import java.util.*;
import interfaces.PriorityRuleInterface;

/*
 * Local Constraint Based Analysis (LCBA) - Ozdamar and Ulusoy 1994
 * This is a simplified implementation focusing on the core precedence analysis
 * TODO
 */
public class LCBA implements PriorityRuleInterface {
    
    @Override
    public Comparator<Integer> getComparator(JobDataInstance data) {
        // Get latest start times for all jobs
        int[] latestStartTimes = DAGLongestPath.generateEarliestAndLatestStartTimes(
            data.jobPredecessors, data.jobDuration, data.horizon)[1];
        
        return new LCBAComparator(data, latestStartTimes);
    }
    
    private static class LCBAComparator implements Comparator<Integer> {
        private final JobDataInstance data;
        private final int[] latestStartTimes;
        private final Map<Integer, Integer> priorities;
        
        public LCBAComparator(JobDataInstance data, int[] latestStartTimes) {
            this.data = data;
            this.latestStartTimes = latestStartTimes;
            this.priorities = computeLCBAPriorities();
        }
        
        private Map<Integer, Integer> computeLCBAPriorities() {
            Map<Integer, Integer> priorities = new HashMap<>();
            
            // Apply simplified LCBA essential conditions
            for (int i = 0; i < data.jobDuration.size(); i++) {
                priorities.put(i, computeJobPriority(i));
            }
            
            return priorities;
        }
        
        private int computeJobPriority(int jobId) {
            int latestFinishTime = latestStartTimes[jobId] + data.jobDuration.get(jobId);
            
            // LCBA priority considers:
            // 1. Latest finish time (primary criterion)
            // 2. Critical path position
            // 3. Resource requirements impact
            
            int criticalPathBonus = 0;
            if (isCriticalJob(jobId)) {
                criticalPathBonus = -1000; // Higher priority for critical jobs
            }
            
            // Consider successor urgency
            int successorUrgency = computeSuccessorUrgency(jobId);
            
            return latestFinishTime + criticalPathBonus - successorUrgency;
        }
        
        private boolean isCriticalJob(int jobId) {
            // Simplified critical path check
            int[] earliestStartTimes = DAGLongestPath.generateEarliestAndLatestStartTimes(
                data.jobPredecessors, data.jobDuration, data.horizon)[0];
            
            int earliestFinish = earliestStartTimes[jobId] + data.jobDuration.get(jobId);
            int latestFinish = latestStartTimes[jobId] + data.jobDuration.get(jobId);
            
            return earliestFinish == latestFinish; // No slack = critical
        }
        
        private int computeSuccessorUrgency(int jobId) {
            // Check urgency of successor jobs
            int maxSuccessorUrgency = 0;
            
            for (int successor = 0; successor < data.jobPredecessors.size(); successor++) {
                if (data.jobPredecessors.get(successor).contains(jobId)) {
                    int successorLatestFinish = latestStartTimes[successor] + data.jobDuration.get(successor);
                    maxSuccessorUrgency = Math.max(maxSuccessorUrgency, 
                        data.horizon - successorLatestFinish);
                }
            }
            
            return maxSuccessorUrgency;
        }
        
        @Override
        public int compare(Integer job1, Integer job2) {
            int priority1 = priorities.get(job1);
            int priority2 = priorities.get(job2);
            
            // Lower priority value = higher actual priority
            int result = Integer.compare(priority1, priority2);
            
            // Tie-breaker by job ID
            if (result == 0) {
                result = Integer.compare(job1, job2);
            }
            
            return result;
        }
    }
}