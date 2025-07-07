package interfaces;

import java.util.Map;

import com.gurobi.gurobi.GRBModel;

import io.JobDataInstance;
import io.Result;

public interface ModelSolutionInterface {
    public GRBModel getModel();
    //public int[][] getEarliestLatestStartTimes();

    /*public static void printResult(ScheduleDoubleResult result) {
    }
    public static void printResult(ScheduleIntegerResult result) {
    } */
}
