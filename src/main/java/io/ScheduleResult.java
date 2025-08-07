package io;

import java.util.List;
import java.util.Map;

public class ScheduleResult {
    private List<String> usedHeuristics;
    // Can contain multiple start times, where job starting times may vary but the makespans are the same
    private List<Map<Integer, Integer>> startTimes;
    private int makespan;

   public ScheduleResult(List<String> usedHeuristics, List<Map<Integer, Integer>> startTimes) {
        this.usedHeuristics = usedHeuristics;
        this.startTimes = startTimes;
        if (startTimes.isEmpty()) {
            this.makespan = Integer.MAX_VALUE; // Default makespan if no start times are provided
        } else {
            this.makespan = startTimes.get(0).get(startTimes.get(0).size() - 1);
        }
   }

    public List<Map<Integer, Integer>> getStartTimes() {
        return startTimes;
    }

    public List<String> getUsedHeuristics() {
        return usedHeuristics;
    }

    public int getMakespan() {
        return makespan;
    }

    public void addHeuristic(String heuristicCode) {
        usedHeuristics.add(heuristicCode);
    }

    public void addStartTimes(Map<Integer,Integer> computedStartTimes) {
        startTimes.add(computedStartTimes);
    }
}

