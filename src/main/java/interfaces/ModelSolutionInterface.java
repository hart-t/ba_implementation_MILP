package interfaces;

import com.gurobi.gurobi.GRBModel;

public interface ModelSolutionInterface {
    public GRBModel getModel();
    //public int[][] getEarliestLatestStartTimes();

    /*public static void printResult(ScheduleDoubleResult result) {
    }
    public static void printResult(ScheduleIntegerResult result) {
    } */
}
