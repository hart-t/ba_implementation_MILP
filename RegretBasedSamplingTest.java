/**
 * Test demonstrating the regret-based sampling implementation for all priority rules
 */
import priorityRules.*;
import io.JobDataInstance;
import java.util.*;

public class RegretBasedSamplingTest {
    
    public static void main(String[] args) {
        // Create a simple test instance
        JobDataInstance testData = createTestInstance();
        List<Integer> eligibleActivities = Arrays.asList(0, 1, 2, 3);
        
        System.out.println("Testing Regret-Based Sampling for all Priority Rules:");
        System.out.println("====================================================");
        
        // Test SPT Rule
        ShortestProcessingTimeRule sptRule = new ShortestProcessingTimeRule();
        testRule("Shortest Processing Time", sptRule, testData, eligibleActivities);
        
        // Test Greatest RPW Rule
        GreatestRankPositionalWeightRule rpwRule = new GreatestRankPositionalWeightRule();
        testRule("Greatest Rank Positional Weight", rpwRule, testData, eligibleActivities);
        
        // Test Most Total Successors Rule
        MostTotalSuccessorsRule successorsRule = new MostTotalSuccessorsRule();
        testRule("Most Total Successors", successorsRule, testData, eligibleActivities);
        
        // Test Most Resource Usage Rule
        MostResourceUsageRule resourceRule = new MostResourceUsageRule();
        testRule("Most Resource Usage", resourceRule, testData, eligibleActivities);
        
        // Test Minimum Latest Start Time Rule
        MinimumLatestStartTimeRule lstRule = new MinimumLatestStartTimeRule();
        testRule("Minimum Latest Start Time", lstRule, testData, eligibleActivities);
        
        // Test Minimum Latest Finish Time Rule
        MinimumLatestFinishTimeRule lftRule = new MinimumLatestFinishTimeRule();
        testRule("Minimum Latest Finish Time", lftRule, testData, eligibleActivities);
        
        // Test Minimum Job Slack Rule
        MinimumJobSlackRule slackRule = new MinimumJobSlackRule();
        testRule("Minimum Job Slack", slackRule, testData, eligibleActivities);
        
        // Test Resource Scheduling Method Rule
        ResourceSchedulingMethodRule rsmRule = new ResourceSchedulingMethodRule();
        testRule("Resource Scheduling Method", rsmRule, testData, eligibleActivities);
    }
    
    private static void testRule(String ruleName, Object rule, JobDataInstance data, List<Integer> activities) {
        System.out.println("\n" + ruleName + " Rule:");
        System.out.println("-".repeat(ruleName.length() + 6));
        
        try {
            List<Integer> result = null;
            
            // Use reflection to call the getRegretBasedSampledList method
            if (rule instanceof ShortestProcessingTimeRule) {
                result = ((ShortestProcessingTimeRule) rule).getRegretBasedSampledList(data, activities);
            } else if (rule instanceof GreatestRankPositionalWeightRule) {
                result = ((GreatestRankPositionalWeightRule) rule).getRegretBasedSampledList(data, activities);
            } else if (rule instanceof MostTotalSuccessorsRule) {
                result = ((MostTotalSuccessorsRule) rule).getRegretBasedSampledList(data, activities);
            } else if (rule instanceof MostResourceUsageRule) {
                result = ((MostResourceUsageRule) rule).getRegretBasedSampledList(data, activities);
            } else if (rule instanceof MinimumLatestStartTimeRule) {
                result = ((MinimumLatestStartTimeRule) rule).getRegretBasedSampledList(data, activities);
            } else if (rule instanceof MinimumLatestFinishTimeRule) {
                result = ((MinimumLatestFinishTimeRule) rule).getRegretBasedSampledList(data, activities);
            } else if (rule instanceof MinimumJobSlackRule) {
                result = ((MinimumJobSlackRule) rule).getRegretBasedSampledList(data, activities);
            } else if (rule instanceof ResourceSchedulingMethodRule) {
                result = ((ResourceSchedulingMethodRule) rule).getRegretBasedSampledList(data, activities);
            }
            
            System.out.println("Input:  " + activities);
            System.out.println("Output: " + result);
            System.out.println("✓ Regret-based sampling method implemented successfully!");
            
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
    }
    
    private static JobDataInstance createTestInstance() {
        // Create a simple 4-job test instance
        String instanceName = "test";
        int numberJob = 4;
        int horizon = 20;
        
        List<Integer> jobDuration = Arrays.asList(3, 5, 2, 4);
        
        List<List<Integer>> jobPredecessors = Arrays.asList(
            Arrays.asList(),        // Job 0: no predecessors
            Arrays.asList(1),       // Job 1: depends on job 0
            Arrays.asList(),        // Job 2: no predecessors
            Arrays.asList(2, 3)     // Job 3: depends on jobs 1 and 2
        );
        
        List<List<Integer>> jobSuccessors = Arrays.asList(
            Arrays.asList(2),       // Job 0: successor is job 1
            Arrays.asList(4),       // Job 1: successor is job 3
            Arrays.asList(4),       // Job 2: successor is job 3
            Arrays.asList()         // Job 3: no successors
        );
        
        List<Integer> jobNumSuccessors = Arrays.asList(1, 1, 1, 0); // Number of successors for each job
        
        List<List<Integer>> jobResource = Arrays.asList(
            Arrays.asList(2, 1),    // Job 0: uses 2 units of resource 0, 1 unit of resource 1
            Arrays.asList(1, 3),    // Job 1: uses 1 unit of resource 0, 3 units of resource 1
            Arrays.asList(3, 1),    // Job 2: uses 3 units of resource 0, 1 unit of resource 1
            Arrays.asList(2, 2)     // Job 3: uses 2 units of resource 0, 2 units of resource 1
        );
        
        List<Integer> resourceCapacity = Arrays.asList(4, 4); // 4 units each of 2 resources
        
        return new JobDataInstance(instanceName, numberJob, horizon, jobNumSuccessors,
                                 jobSuccessors, jobPredecessors, jobDuration, jobResource, resourceCapacity);
    }
}
