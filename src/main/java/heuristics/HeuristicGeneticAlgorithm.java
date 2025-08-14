package heuristics;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import heuristics.geneticAlgorithm.*;
import interfaces.HeuristicInterface;
import io.JobDataInstance;
import io.ScheduleResult;
import utility.DeleteDummyJobs;

public class HeuristicGeneticAlgorithm implements HeuristicInterface {

    private final boolean isOpeningHeuristic = true;
    private final String heuristicCode = enums.HeuristicType.GA.getCode();

    @Override
    public boolean isOpeningHeuristic() {
        return isOpeningHeuristic;
    }

    @Override
    public ScheduleResult determineScheduleResult(JobDataInstance data, ScheduleResult initialScheduleResult) {
        ScheduleResult newSchedule = determineStartTimes(data);
        if (initialScheduleResult.getUsedHeuristics().isEmpty()) {
            System.out.println(heuristicCode + "-" + getPriorityCode() + " found a schedule with makespan " 
                + newSchedule.getMakespan());
            return newSchedule;
        } else {
            // Calculate completion time of last job for both schedules
            int newMakespan = newSchedule.getMakespan();
            int existingMakespan = initialScheduleResult.getMakespan();

            if (newMakespan < existingMakespan) {
                System.out.println(heuristicCode + "-" + getPriorityCode() + " found a better schedule with makespan " + newMakespan + " (was " + existingMakespan + ")");
                return newSchedule;
            } else if (newMakespan == existingMakespan) {
                initialScheduleResult.addHeuristic(heuristicCode + "-" + getPriorityCode());
                if (!initialScheduleResult.startTimesMatch(newSchedule.getStartTimes().get(0))) {
                    initialScheduleResult.addStartTimes(newSchedule.getStartTimes().get(0));
                    System.out.println(heuristicCode + "-" + getPriorityCode() + " found a different schedule with makespan " + newMakespan);
                } else {
                    // System.out.println(heuristicCode + "-" + getPriorityCode() + " found an equivalent schedule with makespan " + newMakespan);
                }
            }
            return initialScheduleResult;
        }
    }

    private ScheduleResult determineStartTimes(JobDataInstance data) {
        JobDataInstance noDummyData = DeleteDummyJobs.deleteDummyJobs(data);
        Individual result = GeneticAlgorithmRunner.solve(noDummyData);

        // Convert the result to a ScheduleResult
        return convertToScheduleResult(result);
    }

    private ScheduleResult convertToScheduleResult(Individual individual) {
        HashSet<String> usedHeuristics = new HashSet<>();
        List<Map<Integer,Integer>> startTimeList = new ArrayList<>();
        startTimeList.add(individual.getStartTimesMap());
        usedHeuristics.add(heuristicCode + "-" + getPriorityCode());

        return new ScheduleResult(usedHeuristics, startTimeList);
    }

    private String getPriorityCode() {
        return enums.PriorityRuleType.MLFT.getCode();
    }
}

