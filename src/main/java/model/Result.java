package model;

import java.util.List;

public class Result {
    public record ScheduleIntegerResult(List<Integer> start, List<Integer> finish) {
    }
    public record ScheduleDoubleResult(List<Integer> start, List<Integer> finish) {
    }
}
