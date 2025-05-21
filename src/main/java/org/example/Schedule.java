package org.example;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a tentative schedule with start times for activities.
 */
public class Schedule {
    private final Map<Integer, Integer> startTimes;

    public Schedule() {
        this.startTimes = new HashMap<>();
    }

    public void setStartTime(int activityId, int start) {
        startTimes.put(activityId, start);
    }

    public int getStartTime(int activityId) {
        return startTimes.getOrDefault(activityId, -1);
    }

    public int makespan() {
        int ms = 0;
        for (Map.Entry<Integer, Integer> entry : startTimes.entrySet()) {
            int id = entry.getKey();
            int st = entry.getValue();
            // find activity
            // for simplicity assume access via global map
        }
        return ms;
    }
}
