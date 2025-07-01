package io;

import java.util.List;

import interfaces.ModelSolutionInterface;

public class Result {
    ModelSolutionInterface modelSolution;
    SolverResults solverResults;

    public Result(ModelSolutionInterface modelSolution, SolverResults solverResults) {
        this.modelSolution = modelSolution;
        this.solverResults = solverResults;
    }

    public record ScheduleIntegerResult(List<Integer> start, List<Integer> finish) {
    }
    public record ScheduleDoubleResult(List<Double> start, List<Double> finish) {
    }
    public static void printResult(ScheduleDoubleResult result) {
        System.out.println("Start times: " + result.start());
        System.out.println("Finish times: " + result.finish());
    }
    public static void printResult(ScheduleIntegerResult result) {
        System.out.println("Start times: " + result.start());
        System.out.println("Finish times: " + result.finish());
    }
    //upper lower bound, time heuristic 

    // 
}
