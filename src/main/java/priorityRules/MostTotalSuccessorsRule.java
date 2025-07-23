package priorityRules;

import io.JobDataInstance;
import java.util.Comparator;
import interfaces.PriorityRuleInterface;

/*
 * Alvarez-Valdes and Tamarit 1993
 */

public class MostTotalSuccessorsRule implements PriorityRuleInterface {
    @Override
    public Comparator<Integer> getComparator(JobDataInstance data) {
        return Comparator.comparingInt(i -> 
            -data.jobSuccessors.get(i).size());
    }
}