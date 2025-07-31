package io;

public class SolverResults {
    double lowerBound;
    double upperBound;
    double objectiveValue;
    double timeInSeconds;
    double mipGap;
    boolean timeLimitReached;
    boolean error;

    public SolverResults(double lowerBound, double upperBound, double objectiveValue, double timeInSeconds, double mipGap, boolean timeLimitReached) {
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.objectiveValue = objectiveValue;
        this.timeInSeconds = timeInSeconds;
        this.mipGap = mipGap;
        this.timeLimitReached = timeLimitReached;
        this.error = objectiveValue < lowerBound;
    }

    public boolean wasStoppedByTimeLimit() {
        return timeLimitReached;
    }

    public boolean hadError() {
        return error;
    }

    public double getMipGap() {
        return mipGap;
    }
}
