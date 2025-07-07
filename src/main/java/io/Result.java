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


    public void printResult() {
        System.out.println("Model Solution:");
        System.out.println("Objective Value: " + solverResults.objectiveValue);
        System.out.println("Lower Bound: " + solverResults.lowerBound);
        System.out.println("Upper Bound: " + solverResults.upperBound);
        System.out.println("Time in seconds: " + solverResults.timeInSeconds);
    }
}
