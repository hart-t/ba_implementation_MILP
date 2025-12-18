package interfaces;

import io.JobDataInstance;
import java.util.*;

import com.gurobi.gurobi.GRBModel;

public interface CompletionMethodInterface {
    public ModelSolutionInterface buildSolution(List<Map<Integer, Integer>> startTimes, JobDataInstance data, GRBModel model, enums.WarmstartStrategy strategy);
}
