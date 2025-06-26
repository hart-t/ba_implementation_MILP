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
        List<Integer> jobNumSuccessors = jobData.jobNumSuccessors;
        List<List<Integer>> jobSuccessors = jobData.jobSuccessors;
        List<List<Integer>> jobPredecessors = jobData.jobPredecessors;
        List<Integer> jobDuration = jobData.jobDuration;
        List<List<Integer>> jobResource = jobData.jobResource;
        List<Integer> resourceCapacity = jobData.resourceCapacity;

        List<Integer> startTimes = new ArrayList<>(Collections.nCopies(numberJob, 0));
        boolean[] scheduled = new boolean[numberJob];

        int[][] resourceUsed = new int[resourceCapacity.size()][horizon];

        // schedule all jobs
        while (!allScheduled(scheduled)) {

            for (int i = 0; i < numberJob; i++) {
                // quit iteration if all jobs are scheduled
                // System.out.println(scheduled[i]);
                if (scheduled[i]) continue;

                boolean allPredecessorsScheduled = true;
                for (int predecessor : jobPredecessors.get(i)) {
                    if (!scheduled[(predecessor - 1)]) {
                        allPredecessorsScheduled = false;

                        break;
                    }
                }

                if (!allPredecessorsScheduled) continue;

                int earliestStart = 0;
                for (int predecessor : jobPredecessors.get(i)) {
                    earliestStart = Math.max(earliestStart, startTimes.get(predecessor - 1) + jobDuration.get(predecessor - 1));
                }

                for (int t = earliestStart; t < horizon; t++) {
                    boolean canSchedule = true;
                    // break if scheduling would exceed the horizon
                    for (int t2 = 0; t2 < jobDuration.get(i); t2++) {
                        if (t + t2 >= horizon) {
                            canSchedule = false;
                            break;
                        }
                        // break if scheduling would exceed the resource capacity
                        for (int r = 0; r < resourceCapacity.size(); r++) {
                            if (resourceUsed[r][t] + jobResource.get(i).get(r) > resourceCapacity.get(r)) {
                                // System.out.println(available[r] + jobResource.get(i).get(r) > resourceCapacity.get(r));
                                canSchedule = false;
                                break;
                            }
                        }
                        if (!canSchedule) break;
                    }

                    if (canSchedule) {
                        for (int t2 = 0; t2 < jobDuration.get(i); t2++) {
                            for (int r = 0; r < resourceCapacity.size(); r++) {
                                if (t + t2 < horizon) {
                                    resourceUsed[r][t] += jobResource.get(i).get(r);
                                }
                            }
                        }
                        startTimes.set(i, t);
                        scheduled[i] = true;
                        break;
                    }
                }
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
}
