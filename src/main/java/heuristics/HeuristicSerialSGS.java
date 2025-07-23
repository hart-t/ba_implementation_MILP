package heuristics;

import java.util.*;

import io.JobDataInstance;
import interfaces.HeuristicInterface;
import interfaces.PriorityRuleInterface;
import solutionBuilder.BuildFlowSolution;

/*
 * Heuristic Serial SGS (Serial Schedule Generation Scheme)
 * https://www.hsba.de/fileadmin/user_upload/bereiche/_dokumente/6-forschung/profs-publikationen/Hartmann_1999_Heuristic_Algorithms_for_solving_the_resource-constrained_project_scheduling_problem.pdf
 * priority rule: shortest processing time first
 */

public class HeuristicSerialSGS implements HeuristicInterface {
    
    private final PriorityRuleInterface priorityStrategy;

    public HeuristicSerialSGS(PriorityRuleInterface priorityStrategy) {
        this.priorityStrategy = priorityStrategy;
    }

    @Override
    public Map<Integer, Integer> determineStartTimes(JobDataInstance data) {
        int numberJob = data.numberJob;
        int horizon = data.horizon;
        List<List<Integer>> jobPredecessors = data.jobPredecessors;
        List<Integer> jobDuration = data.jobDuration;
        List<List<Integer>> jobResource = data.jobResource;
        List<Integer> resourceCapacity = data.resourceCapacity;

        Map<Integer, Integer> startTimes = new HashMap<>();
        // Initialize all start times to 0
        for (int i = 0; i < numberJob; i++) {
            startTimes.put(i, 0);
        }
        boolean[] scheduled = new boolean[numberJob];

        int[][] resourceUsed = new int[resourceCapacity.size()][horizon];

        // schedule all jobs
        int maxIterations = numberJob * 2; // Safety limit
        int iterationCount = 0;
        
        while (!allScheduled(scheduled) && iterationCount < maxIterations) {
            iterationCount++;
            
            // Find all eligible activities (precedence-feasible)
            List<Integer> eligibleActivities = new ArrayList<>();
            
            for (int i = 0; i < numberJob; i++) {
                if (scheduled[i]) continue;

                boolean allPredecessorsScheduled = true;
                for (int predecessor : jobPredecessors.get(i)) {
                    if (!scheduled[(predecessor - 1)]) {
                        allPredecessorsScheduled = false;
                        break;
                    }
                }

                if (allPredecessorsScheduled) {
                    eligibleActivities.add(i);
                }
            }
            
            // Apply priority rule
            eligibleActivities.sort(priorityStrategy.getComparator(data));
            
            // Schedule the highest priority eligible activity
            for (int i : eligibleActivities) {
                int earliestStart = 0;
                for (int predecessor : jobPredecessors.get(i)) {
                    earliestStart = Math.max(earliestStart, startTimes.get(predecessor - 1) + jobDuration.get(predecessor - 1));
                }

                boolean activityScheduled = false;
                // Current logic only checks if resources fit at time t
                for (int t = earliestStart; t < horizon && !activityScheduled; t++) {
                    boolean canSchedule = true;
                    for (int t2 = 0; t2 < jobDuration.get(i); t2++) {
                        for (int r = 0; r < resourceCapacity.size(); r++) {
                            if (t + t2 < horizon) {
                                // BUG: This checks if adding this job exceeds capacity
                                // But it should prevent scheduling if it would exceed capacity
                                if (resourceUsed[r][t + t2] + jobResource.get(i).get(r) > resourceCapacity.get(r)) {
                                    canSchedule = false;
                                    break;
                                }
                            }
                        }
                        if (!canSchedule) break;
                    }
                    
                    // Only schedule if resource constraints are satisfied
                    if (canSchedule) {
                        // Update resource usage
                        for (int t2 = 0; t2 < jobDuration.get(i); t2++) {
                            for (int r = 0; r < resourceCapacity.size(); r++) {
                                if (t + t2 < horizon) {
                                    resourceUsed[r][t + t2] += jobResource.get(i).get(r);
                                }
                            }
                        }
                        startTimes.put(i, t);
                        scheduled[i] = true;
                        activityScheduled = true;
                        break;
                    }
                }
                
                if (activityScheduled) break; // Only schedule one activity per iteration
            }
            
            // If no activity was scheduled in this iteration, we have a problem lol
            boolean anyActivityScheduled = false;
            for (int i : eligibleActivities) {
                if (scheduled[i]) {
                    anyActivityScheduled = true;
                    break;
                }
            }
            
            if (!anyActivityScheduled) {
                throw new RuntimeException("SSGS failed to schedule any activity in iteration " + iterationCount);
            }
        }
        
        if (!allScheduled(scheduled)) {
            throw new RuntimeException("SSGS failed to schedule all activities within iteration limit");
        }

        // After scheduling, check total resource usage
        boolean hasViolations = false;
        for (int r = 0; r < resourceCapacity.size(); r++) {
            int maxUsage = 0;
            for (int t = 0; t < horizon; t++) {
                maxUsage = Math.max(maxUsage, resourceUsed[r][t]);
            }
            if (maxUsage > resourceCapacity.get(r)) {
                if (!hasViolations) {
                    System.out.println("=== HEURISTIC RESOURCE VIOLATIONS ===");
                    hasViolations = true;
                }
                System.out.println("Resource " + r + ": max usage=" + maxUsage + 
                                 ", capacity=" + resourceCapacity.get(r) + " ✗ VIOLATED");
            }
        }

        return startTimes;
    }

    private static boolean allScheduled(boolean[] scheduled) {
        for (boolean s : scheduled) {
            if (!s) return false;
        }
        return true;
    }

    @Override
    public Map<Integer, Integer> determineStartTimes(JobDataInstance data, Map<Integer, Integer> initialStartTimes) {
        // This method is not implemented in this heuristic
        throw new UnsupportedOperationException("This heuristic does not support initial start times.");
    }
}

