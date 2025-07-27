package interfaces;

import io.JobDataInstance;
import io.ScheduleResult;

public interface HeuristicInterface {

    /*
     * returns true if the heuristic is an opening heuristic
     */
    public boolean isOpeningHeuristic();

    /*
     * returns a ScheduleResult
     */
    public ScheduleResult determineScheduleResult(JobDataInstance data, ScheduleResult initialStartTimes);
}
