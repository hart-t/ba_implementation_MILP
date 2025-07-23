package interfaces;

import io.JobDataInstance;
import java.util.Comparator;

public interface PriorityRuleInterface {
    Comparator<Integer> getComparator(JobDataInstance data);
}