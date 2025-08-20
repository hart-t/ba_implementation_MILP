package io;

import java.util.ArrayList;
import java.util.List;

public class CallbackValues {
    private List<Double> objValues = new ArrayList<>();
    private List<Double> times = new ArrayList<>();
    private List<Integer> solutions = new ArrayList<>();

    public void addValues(double objValue, double time, int solution) {
        objValues.add(objValue);
        times.add(time);
        solutions.add(solution);
    }

    public List<Double> getObjValues() {
        return objValues;
    }

    public List<Double> getTimes() {
        return times;
    }

    public List<Integer> getSolutions() {
        return solutions;
    }
}
