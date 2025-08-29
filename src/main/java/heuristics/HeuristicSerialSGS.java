package heuristics;

import java.util.*;

import io.JobDataInstance;
import interfaces.HeuristicInterface;
import interfaces.PriorityRuleInterface;
import interfaces.SamplingTypeInterface;
import io.ScheduleResult;

/*
 * Heuristic Serial SGS (Serial Schedule Generation Scheme)
 * https://www.hsba.de/fileadmin/user_upload/bereiche/_dokumente/6-forschung/profs-publikationen/Hartmann_1999_Heuristic_Algorithms_for_solving_the_resource-constrained_project_scheduling_problem.pdf
 * priority rule: shortest processing time first
 */

public class HeuristicSerialSGS implements HeuristicInterface {
    
    private final PriorityRuleInterface priorityStrategy;
    private final boolean isOpeningHeuristic = true;
    private final String heuristicCode = enums.HeuristicType.SSGS.getCode(); // Serial Schedule Generation Scheme
    private SamplingTypeInterface samplingType;

    public HeuristicSerialSGS(PriorityRuleInterface priorityStrategy, SamplingTypeInterface samplingType) {
        this.priorityStrategy = priorityStrategy;
        this.samplingType = samplingType;
    }

    private ScheduleResult determineStartTimes(JobDataInstance data) {
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

            switch (samplingType.getSamplingType()) {
                case enums.SamplingType.NS:
                    eligibleActivities.sort(priorityStrategy.getComparator(data));
                    break;

                case enums.SamplingType.RS:
                    Collections.shuffle(eligibleActivities);
                    break;

                case enums.SamplingType.BRS:
                    eligibleActivities = priorityStrategy.getSampledList(data, eligibleActivities);
                    break;

                case enums.SamplingType.RBRS:
                    eligibleActivities = priorityStrategy.getRegretBasedSampledList(data, eligibleActivities);
                    break;
            }

            // Schedule the highest priority eligible activity
            boolean activityScheduledThisIteration = false;
            
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
                                // it should prevent scheduling if it would exceed capacity
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
                        activityScheduledThisIteration = true;
                        break;
                    }
                }
                
                if (activityScheduled) break; // Only schedule one activity per iteration
            }
            
            // If no activity was scheduled in this iteration, we have a problem
            if (!activityScheduledThisIteration) {
                throw new RuntimeException(heuristicCode + "-" + getPriorityCode() + " failed to schedule any activity in iteration " + iterationCount);
            }
        }
        
        if (!allScheduled(scheduled)) {
            throw new RuntimeException(heuristicCode + "-" + getPriorityCode() + " failed to schedule all activities within iteration limit");
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
        HashSet<String> usedHeuristics = new HashSet<>();
        List<Map<Integer,Integer>> startTimeList = new ArrayList<>();
        startTimeList.add(startTimes);
        usedHeuristics.add(heuristicCode + "-" + getPriorityCode());

        return new ScheduleResult(usedHeuristics, startTimeList);
    }

    private static boolean allScheduled(boolean[] scheduled) {
        for (boolean s : scheduled) {
            if (!s) return false;
        }
        return true;
    }

    @Override
    public boolean isOpeningHeuristic() {
        return isOpeningHeuristic;
    }

    @Override
    public ScheduleResult determineScheduleResult(JobDataInstance data, ScheduleResult initialScheduleResult) {
        ScheduleResult newSchedule = determineStartTimes(data);
        if (initialScheduleResult.getUsedHeuristics().isEmpty()) {
            System.out.println(heuristicCode + "-" + getPriorityCode() + " found a schedule with makespan " 
                + newSchedule.getMakespan());
            return newSchedule;
        } else {
            // Calculate completion time of last job for both schedules
            int newMakespan = newSchedule.getMakespan();
            int existingMakespan = initialScheduleResult.getMakespan();

            if (newMakespan < existingMakespan) {
                System.out.println(heuristicCode + "-" + getPriorityCode() + " found a better schedule with makespan " + newMakespan + " (was " + existingMakespan + ")");
                return newSchedule;
            } else if (newMakespan == existingMakespan) {
                initialScheduleResult.addHeuristic(heuristicCode + "-" + getPriorityCode());
                if (!initialScheduleResult.startTimesMatch(newSchedule.getStartTimes().get(0))) {
                    initialScheduleResult.addStartTimes(newSchedule.getStartTimes().get(0));
                    System.out.println(heuristicCode + "-" + getPriorityCode() + " found a different schedule with makespan " + newMakespan);
                } else {
                    // System.out.println(heuristicCode + "-" + getPriorityCode() + " found an equivalent schedule with makespan " + newMakespan);
                }
            }
            return initialScheduleResult;
        }
    }
        
    private String getPriorityCode() {
        // TODO
        // i need to store the priority rule type or extract it from the priorityStrategy
        // For now, this is a placeholder - i might need to modify the constructor
        return priorityStrategy.getClass().getSimpleName().replace("Rule", "").toUpperCase();
    }

    @Override
    public String getHeuristicCode() {
        return heuristicCode;
    }
}

