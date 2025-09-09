package io;

import java.util.Map;

public class SolverResults {
    double lowerBound;
    double upperBound;
    double objectiveValue;
    double timeInSeconds;
    double mipGap;
    boolean timeLimitReached;
    boolean error;
    Map<Integer, Integer> targetFunctionValueCurve;

    public SolverResults(double lowerBound, double upperBound, double objectiveValue, double timeInSeconds, double mipGap, boolean timeLimitReached) {
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.objectiveValue = objectiveValue;
        this.timeInSeconds = timeInSeconds;
        this.mipGap = mipGap;
        this.timeLimitReached = timeLimitReached;
        this.error = objectiveValue < lowerBound;
    }

    // Add this new constructor that accepts the targetFunctionValueCurve
    public SolverResults(double lowerBound, double upperBound, double objectiveValue, double timeInSeconds, double mipGap, boolean timeLimitReached, Map<Integer, Integer> targetFunctionValueCurve) {
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.objectiveValue = objectiveValue;
        this.timeInSeconds = timeInSeconds;
        this.mipGap = mipGap;
        this.timeLimitReached = timeLimitReached;
        this.error = objectiveValue < lowerBound;
        this.targetFunctionValueCurve = targetFunctionValueCurve;
    }

    public boolean wasStoppedByTimeLimit() {
        return timeLimitReached;
    }

    public boolean hadError() {
        return error;
    }

    public void setTargetFunctionValueCurve(Map<Integer, Integer> targetFunctionValueCurve) {
        this.targetFunctionValueCurve = targetFunctionValueCurve;
    }

    // Add getter method for the targetFunctionValueCurve
    public Map<Integer, Integer> getTargetFunctionValueCurve() {
        return targetFunctionValueCurve;
    }

    public double getMipGap() {
        return mipGap;
    }
}
