package priorityRules;

import io.JobDataInstance;
import java.util.Comparator;
import interfaces.PriorityRuleInterface;
import static utility.DAGLongestPath.generateEarliestAndLatestStartTimes;

/*
 *  Minimum Job Slack Rule wont work with continuous updates and the Comparator i think
 *  https://www.jstor.org/stable/2629856?seq=5
 */

public class MinimumJobSlackRule implements PriorityRuleInterface {
    @Override
    public Comparator<Integer> getComparator(JobDataInstance data) {
        int[][] earliestAndLatestStartTimes = generateEarliestAndLatestStartTimes
                (data.jobPredecessors, data.jobDuration, data.horizon);
        int[] earliestStartTimes = earliestAndLatestStartTimes[0];
        int[] latestStartTimes = earliestAndLatestStartTimes[1];
        
        return Comparator.comparingInt(jobId -> {
            // Calculate slack as LST - EST
            int slack = latestStartTimes[jobId] - earliestStartTimes[jobId];
            return slack;
        });
    }
}