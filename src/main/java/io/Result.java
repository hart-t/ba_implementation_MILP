package io;

import java.util.HashSet;
import java.util.List;

import enums.ModelType;
import interfaces.ModelSolutionInterface;

public class Result {
    ModelSolutionInterface modelSolution;
    SolverResults solverResults;
    int[][] startAndFinishTimes;
    ScheduleResult scheduleResult;
    String instanceName;

    public Result(ModelSolutionInterface modelSolution, SolverResults solverResults, int[][] startAndFinishTimes, ScheduleResult scheduleResult, String instanceName) {
        this.modelSolution = modelSolution;
        this.solverResults = solverResults;
        this.startAndFinishTimes = startAndFinishTimes;
        this.scheduleResult = scheduleResult;
        this.instanceName = instanceName;
    }


    public void printResult() {
        System.out.println("Model Solution:");
        System.out.println("Objective Value: " + solverResults.objectiveValue);
        System.out.println("Lower Bound: " + solverResults.lowerBound);
        System.out.println("Upper Bound: " + solverResults.upperBound);
        System.out.println("Time in seconds: " + solverResults.timeInSeconds);

        System.out.println("Model Solution Details:");
        
        if (startAndFinishTimes != null && startAndFinishTimes.length >= 2) {
            int[] startTimes = startAndFinishTimes[0];
            int[] finishTimes = startAndFinishTimes[1];
            
            System.out.println("\nJob Schedule:");
            System.out.println("Job\tStart Time\tFinish Time");
            System.out.println("----\t----------\t-----------");
            
            for (int i = 0; i < startTimes.length; i++) {
                if (startTimes[i] >= 0) {
                    System.out.println((i + 1) + "\t" + startTimes[i] + "\t\t" + finishTimes[i]);
                } else {
                    System.out.println((i + 1) + "\tNot scheduled\tNot scheduled");
                }
            }
        } else {
            System.out.println("No start and finish times available.");
        }
    }

    public HashSet<String> getUsedHeuristics() {
            return scheduleResult.getUsedHeuristics();
    }

    public int getBestHeuristicMakespan() {
        return scheduleResult.getMakespan();
    }

    public ModelType getModelType() {
        return modelSolution.getModelType();
    }
}
