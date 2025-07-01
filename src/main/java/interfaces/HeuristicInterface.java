package interfaces;

import java.util.Map;

import io.JobDataInstance;

public interface HeuristicInterface {

    public Map<Integer, Integer> determineStartTimes(JobDataInstance data);
}
