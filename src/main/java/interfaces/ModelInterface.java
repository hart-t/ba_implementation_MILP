package interfaces;

import io.JobDataInstance;
import com.gurobi.gurobi.*;

public interface ModelInterface {
    public GRBModel completeModel(ModelSolutionInterface initialSolution, JobDataInstance data);
    public CompletionMethodInterface getCompletionMethod();
    public boolean usesDummyJobs();
    public int[][] getStartAndFinishTimes(GRBModel model, JobDataInstance data);
}