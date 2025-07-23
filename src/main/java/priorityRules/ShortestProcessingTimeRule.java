package priorityRules;

import io.JobDataInstance;
import java.util.Comparator;
import interfaces.PriorityRuleInterface;

public class ShortestProcessingTimeRule implements PriorityRuleInterface {
    @Override
    public Comparator<Integer> getComparator(JobDataInstance data) {
        return Comparator.comparingInt(i -> data.jobDuration.get(i));
    }
}