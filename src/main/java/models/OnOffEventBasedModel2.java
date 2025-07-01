/*package models;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.gurobi.gurobi.GRB;
import com.gurobi.gurobi.GRBLinExpr;
import com.gurobi.gurobi.GRBModel;
import com.gurobi.gurobi.GRBVar;

import io.FileReader;

/**
 * On off Event-Based Model 2
 *
 * https://www.sciencedirect.com/science/article/pii/S0305054809003360
 *      on off event-based model
 *
 *      public final int numberJob;
 *      public final int horizon;
 *      public final List<Integer> jobNumSuccessors;
 *      public final List<List<Integer>> jobSuccessors;
 *      public final List<List<Integer>> jobPredecessors;
 *      public final List<Integer> jobDuration;
 *      public final List<List<Integer>> jobResource;
 *      public final List<Integer> resourceCapacity;
 * 
 *      In this model, the number of events is exactly equal to the number of activities, n.
 * */

/*public class OnOffEventBasedModel2 {
    
    public static GRBModel gurobiRcpspJ30(GRBModel model,
     FileReader.JobData data ) throws Exception {

        // generate TE matrix, This means that if there is a path from activity i to j in the precedence graph
        // (even if not directly), then (i,j) element TE.
        int[][] teMatrix = computeTEMatrix(data.numberJob, data.jobSuccessors, data.jobDuration);

        /*for (int[] row : teMatrix) {
            System.out.println(Arrays.toString(row));
        }*/

        /* 
        int[][] startTimes = generateEarliestAndLatestStartTimes(data.jobPredecessors, data.jobDuration, data.horizon);
        int[] earliestStartTime = startTimes[0];
        int[] latestStartTime = startTimes[1];

        // add zie
        GRBVar[][] jobActiveAtEvent = new GRBVar[data.numberJob][data.numberJob];
        for (int i = 0; i < data.numberJob; i++) {
            for (int e = 0; e < data.numberJob; e++) {
                jobActiveAtEvent[i][e] = model.addVar(0.0, data.horizon, 0.0, GRB.CONTINUOUS, "startingTime[" +
                    i + "]");
            }
        }

        GRBVar[] startOfEventE = new GRBVar[data.numberJob * data.numberJob];
        for (int i = 0; i < startOfEventE.length; i++) {
            
        }
        
     }

}
*/