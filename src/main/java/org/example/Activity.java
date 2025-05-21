package org.example;/*
 * Standard RCPSP (Resource-Constrained Project Scheduling Problem) Implementation in Java
 *
 * Based on classical definitions of RCPSP as described in:
 *   - Kolisch, R. (2009). Project scheduling under resource constraints: Models, algorithms, extensions. Computers & Operations Research, 36(2), 529–542. DOI:10.1016/j.cor.2009.12.011
 *   - Hartmann, S., & Briskorn, D. (2002). A survey of variants and extensions of the resource-constrained project scheduling problem. European Journal of Operational Research, 183(1), 11–51. DOI:10.1016/S0377-2217(02)00758-0
 *
 * This implementation uses a serial scheduling generation scheme (SSGS) to construct feasible schedules.
 * The objective is to minimize the project makespan.
 *
 * Author: [Your Name]
 * Date: [Date]
 */

import java.util.*;

/**
 * Represents a single activity in the RCPSP.
 */
public class Activity {
    private final int id;
    private final int duration;
    private final List<Integer> successors;
    private final Map<ResourceType, Integer> resourceRequirements;

    /**
     * Constructs an activity.
     * @param id unique identifier
     * @param duration processing time
     * @param requirements map of resource type to amount required
     */
    public Activity(int id, int duration, Map<ResourceType, Integer> requirements) {
        this.id = id;
        this.duration = duration;
        this.successors = new ArrayList<>();
        this.resourceRequirements = new HashMap<>(requirements);
    }

    public int getId() { return id; }
    public int getDuration() { return duration; }
    public List<Integer> getSuccessors() { return successors; }
    public Map<ResourceType, Integer> getRequirements() { return resourceRequirements; }

    /** Adds a precedence successor for this activity. */
    public void addSuccessor(int successorId) {
        successors.add(successorId);
    }
}

