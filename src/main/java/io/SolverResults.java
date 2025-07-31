package io;

public class SolverResults {
    double lowerBound;
    double upperBound;
    double objectiveValue;
    double timeInSeconds;

    public SolverResults(double lowerBound, double upperBound, double objectiveValue, double timeInSeconds) {
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.objectiveValue = objectiveValue;
        this.timeInSeconds = timeInSeconds;
    }

    public boolean wasStoppedByTimeLimit() {
        return false; // Placeholder for actual implementation
    }
}
