package heuristics.priorityRules;

import io.JobDataInstance;
import java.util.Comparator;
import interfaces.PriorityRuleInterface;

public class MostResourceUsageRule implements PriorityRuleInterface {
    @Override
    public Comparator<Integer> getComparator(JobDataInstance data) {
        return Comparator.comparingInt(i -> 
            -data.jobResource.get(i).stream().mapToInt(Integer::intValue).sum());
    }
}