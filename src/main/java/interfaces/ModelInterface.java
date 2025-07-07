package interfaces;

import io.JobDataInstance;
import com.gurobi.gurobi.*;
import io.Result;

public interface ModelInterface {
    public GRBModel completeModel(ModelSolutionInterface initialSolution, JobDataInstance data);
    public CompletionMethodInterface getCompletionMethod();
    public boolean usesDummyJobs();
}