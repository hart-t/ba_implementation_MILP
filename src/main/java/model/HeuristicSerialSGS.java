package model;

import java.util.*;

/*
 * Heuristic Serial SGS (Serial Schedule Generation Scheme)
 * https://www.hsba.de/fileadmin/user_upload/bereiche/_dokumente/6-forschung/profs-publikationen/Hartmann_1999_Heuristic_Algorithms_for_solving_the_resource-constrained_project_scheduling_problem.pdf
 */

public class HeuristicSerialSGS {

    public static List<Integer> serialSGS(FileReader.JobData jobData) {
        int numberJob = jobData.numberJob;
        int horizon = jobData.horizon;
        List<List<Integer>> jobPredecessors = jobData.jobPredecessors;
        List<Integer> jobDuration = jobData.jobDuration;
        List<List<Integer>> jobResource = jobData.jobResource;
        List<Integer> resourceCapacity = jobData.resourceCapacity;

        List<Integer> startTimes = new ArrayList<>(Collections.nCopies(numberJob, 0));
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
            
            // Apply priority rule (shortest processing time first)
            eligibleActivities.sort(Comparator.comparingInt(i -> jobDuration.get(i)));
            
            // Schedule the highest priority eligible activity
            for (int i : eligibleActivities) {
                int earliestStart = 0;
                for (int predecessor : jobPredecessors.get(i)) {
                    earliestStart = Math.max(earliestStart, startTimes.get(predecessor - 1) + jobDuration.get(predecessor - 1));
                }

                boolean activityScheduled = false;
                for (int t = earliestStart; t < horizon && !activityScheduled; t++) {
                    boolean canSchedule = true;
                    
                    // Check resource feasibility for entire duration
                    for (int t2 = 0; t2 < jobDuration.get(i); t2++) {
                        if (t + t2 >= horizon) {
                            canSchedule = false;
                            break;
                        }
                        
                        for (int r = 0; r < resourceCapacity.size(); r++) {
                            if (resourceUsed[r][t + t2] + jobResource.get(i).get(r) > resourceCapacity.get(r)) {
                                canSchedule = false;
                                break;
                            }
                        }
                        if (!canSchedule) break;
                    }

                    if (canSchedule) {
                        // Update resource usage for entire duration
                        for (int t2 = 0; t2 < jobDuration.get(i); t2++) {
                            for (int r = 0; r < resourceCapacity.size(); r++) {
                                if (t + t2 < horizon) {
                                    resourceUsed[r][t + t2] += jobResource.get(i).get(r);
                                }
                            }
                        }
                        startTimes.set(i, t);
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
        return startTimes;
    }

    private static boolean allScheduled(boolean[] scheduled) {
        for (boolean s : scheduled) {
            if (!s) return false;
        }
        return true;
    }
}
