package org.example;

import java.util.List;
import java.util.Map;

/**
 * RCPSP problem definition.
 */
public class RCPSPProblem {
    private final List<Activity> activities;
    private final Map<ResourceType, Integer> resourceCapacities;

    public RCPSPProblem(List<Activity> activities, Map<ResourceType, Integer> capacities) {
        this.activities = activities;
        this.resourceCapacities = capacities;
    }

    public List<Activity> getActivities() { return activities; }
    public Map<ResourceType, Integer> getCapacities() { return resourceCapacities; }
}
