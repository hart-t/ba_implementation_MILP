package priorityRules;

import io.JobDataInstance;
import java.util.Comparator;
import interfaces.PriorityRuleInterface;
import static utility.DAGLongestPath.generateEarliestAndLatestStartTimes;

/*
 * Resource Scheduling Method (RSM) Rule
 * This rule was developed at the University of Illinois by Brand, Meyer and Shaffer,
 * who reported it most effective in a series of tests conducted on construction-industry
 * projects
 *
 *     https://www.jstor.org/stable/2629856?seq=5
 */

public class ResourceSchedulingMethodRule implements PriorityRuleInterface {
    @Override
    public Comparator<Integer> getComparator(JobDataInstance data) {
        int[][] earliestAndLatestStartTimes = generateEarliestAndLatestStartTimes
                (data.jobPredecessors, data.jobDuration, data.horizon);
        int[] earliestStartTimes = earliestAndLatestStartTimes[0];
        int[] latestStartTimes = earliestAndLatestStartTimes[1];
        
        return Comparator.comparingInt(i -> {
            int minDij = Integer.MAX_VALUE;
            int eftI = earliestStartTimes[i] + data.jobDuration.get(i); // Earliest Finish Time of activity i

            // Compare with all other jobs to find minimum d_ij
            for (int j = 0; j < data.numberJob; j++) {
                if (i != j) {
                    int lstJ = latestStartTimes[j]; // Late Start Time of activity j
                    int dij = Math.max(0, eftI - lstJ);
                    minDij = Math.min(minDij, dij);
                }
            }
            
            return minDij == Integer.MAX_VALUE ? 0 : minDij;
        });
    }
}