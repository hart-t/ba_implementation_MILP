package priorityRules;

import io.JobDataInstance;
import java.util.*;
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

    @Override
    public List<Integer> getSampledList(JobDataInstance data, List<Integer> eligibleActivities) {
        if (eligibleActivities.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Calculate slack values for all eligible activities
        int[][] earliestAndLatestStartTimes = generateEarliestAndLatestStartTimes
                (data.jobPredecessors, data.jobDuration, data.horizon);
        int[] earliestStartTimes = earliestAndLatestStartTimes[0];
        int[] latestStartTimes = earliestAndLatestStartTimes[1];
        
        Map<Integer, Integer> slackValues = new HashMap<>();
        int maxSlack = Integer.MIN_VALUE;
        int minSlack = Integer.MAX_VALUE;
        
        for (int job : eligibleActivities) {
            int slack = latestStartTimes[job] - earliestStartTimes[job];
            slackValues.put(job, slack);
            maxSlack = Math.max(maxSlack, slack);
            minSlack = Math.min(minSlack, slack);
        }
        
        List<Integer> result = new ArrayList<>();
        List<Integer> remaining = new ArrayList<>(eligibleActivities);
        Random random = new Random();
        
        while (!remaining.isEmpty()) {
            boolean jobSelectedThisIteration = false;
            
            for (int i = 0; i < remaining.size(); i++) {
                int job = remaining.get(i);
                int slack = slackValues.get(job);
                
                // Calculate probability: (maxSlack - slack + 1) / (maxSlack - minSlack + 1)
                // Jobs with smaller slack (less flexibility) get higher probability
                double probability;
                if (maxSlack == minSlack) {
                    probability = 1.0 / remaining.size(); // Equal probability if all have same slack
                } else {
                    probability = (double)(maxSlack - slack + 1) / (maxSlack - minSlack + 1);
                }
                
                if (random.nextDouble() < probability) {
                    result.add(job);
                    remaining.remove(i);
                    jobSelectedThisIteration = true;
                    break;
                }
            }
            
            // Fallback: if no job was selected, select the one with minimum slack
            if (!jobSelectedThisIteration) {
                int bestJob = remaining.stream()
                    .min(Comparator.comparingInt(slackValues::get))
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
            
            // Calculate slack values for remaining activities
            int[][] earliestAndLatestStartTimes = generateEarliestAndLatestStartTimes
                    (data.jobPredecessors, data.jobDuration, data.horizon);
            int[] earliestStartTimes = earliestAndLatestStartTimes[0];
            int[] latestStartTimes = earliestAndLatestStartTimes[1];
            
            // Find the minimum slack among remaining activities (best choice for this rule)
            int minSlack = remaining.stream()
                .mapToInt(job -> latestStartTimes[job] - earliestStartTimes[job])
                .min()
                .orElse(Integer.MAX_VALUE);
            
            // Calculate regret for each activity (difference from minimum)
            for (int job : remaining) {
                int slack = latestStartTimes[job] - earliestStartTimes[job];
                double regret = slack - minSlack;
                regrets.add(regret);
                totalRegret += regret;
            }
            
            // If all activities have the same slack (totalRegret = 0), select randomly
            if (totalRegret == 0.0) {
                int selectedIndex = random.nextInt(remaining.size());
                result.add(remaining.remove(selectedIndex));
                continue;
            }
            
            // Calculate selection probabilities based on inverse regret
            // Activities with lower regret (closer to minimum) have higher probability
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