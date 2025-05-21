package org.example;

import java.util.*;

/**
 * Serial Schedule Generation Scheme (SSGS) implementation.
 */
public class SSGSSolver {
    private final RCPSPProblem problem;
    private final Schedule schedule;

    public SSGSSolver(RCPSPProblem problem) {
        this.problem = problem;
        this.schedule = new Schedule();
    }

    /**
     * Executes the SSGS to produce a feasible schedule.
     *
     * @return schedule with assigned start times
     */
    public Schedule solve() {
        List<Activity> sorted = precedenceList();
        Map<ResourceType, int[]> resourceProfile = initializeProfile();

        for (Activity a : sorted) {
            int est = computeEarliestStart(a);
            int start = findEarliestFeasible(est, a, resourceProfile);
            schedule.setStartTime(a.getId(), start);
            updateProfile(start, a, resourceProfile);
        }

        return schedule;
    }

    /**
     * Topologically sorts activities by precedence.
     */
    private List<Activity> precedenceList() {
        // implement Kahn's algorithm
        return new ArrayList<>(problem.getActivities());
    }

    /**
     * Initializes resource usage profile over time horizon.
     */
    private Map<ResourceType, int[]> initializeProfile() {
        Map<ResourceType, int[]> profile = new HashMap<>();
        int horizon = computeHorizon();
        for (Map.Entry<ResourceType, Integer> cap : problem.getCapacities().entrySet()) {
            profile.put(cap.getKey(), new int[horizon]);
            Arrays.fill(profile.get(cap.getKey()), cap.getValue());
        }
        return profile;
    }

    private int computeHorizon() {
        int sumDur = 0;
        for (Activity a : problem.getActivities()) sumDur += a.getDuration();
        return sumDur;
    }

    /**
     * Computes earliest start respecting precedence only.
     */
    private int computeEarliestStart(Activity a) {
        int est = 0;
        // for each pred compute finish, take max
        return est;
    }

    /**
     * Scans resource profile to find first time >= est where requirements fit.
     */
    private int findEarliestFeasible(int est, Activity a, Map<ResourceType, int[]> profile) {
        int t = est;
        while (true) {
            if (fits(t, a, profile)) return t;
            t++;
        }
    }

    private boolean fits(int t, Activity a, Map<ResourceType, int[]> profile) {
        for (Map.Entry<ResourceType, Integer> req : a.getRequirements().entrySet()) {
            ResourceType r = req.getKey();
            int need = req.getValue();
            int[] avail = profile.get(r);
            for (int tau = t; tau < t + a.getDuration(); tau++) {
                if (tau >= avail.length || avail[tau] < need) return false;
            }
        }
        return true;
    }

    private void updateProfile(int start, Activity a, Map<ResourceType, int[]> profile) {
        for (Map.Entry<ResourceType, Integer> req : a.getRequirements().entrySet()) {
            ResourceType r = req.getKey();
            int need = req.getValue();
            int[] avail = profile.get(r);
            for (int tau = start; tau < start + a.getDuration(); tau++) {
                avail[tau] -= need;
            }
        }
    }
}
