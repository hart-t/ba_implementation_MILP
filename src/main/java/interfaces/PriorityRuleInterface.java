package interfaces;

import io.JobDataInstance;
import java.util.Comparator;
import java.util.*;

public interface PriorityRuleInterface {
    Comparator<Integer> getComparator(JobDataInstance data);
    List<Integer> getSampledList(JobDataInstance data, List<Integer> eligibleActivities);
    List<Integer> getRegretBasedSampledList(JobDataInstance data, List<Integer> eligibleActivities);
}