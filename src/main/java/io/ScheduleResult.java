package io;

import java.util.List;
import java.util.Map;

public class ScheduleResult {
    public List<String> usedHeuristics;
    public Map<Integer, Integer> startTimes;

   public ScheduleResult(List<String> usedHeuristics, Map<Integer, Integer> startTimes) {
       this.usedHeuristics = usedHeuristics;
       this.startTimes = startTimes;
   }

    public Map<Integer, Integer> getStartTimes() {
        return startTimes;
    }

    public List<String> getUsedHeuristics() {
        return usedHeuristics;
    }
}

