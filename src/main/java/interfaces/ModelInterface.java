package interfaces;

import io.JobDataInstance;
import io.Result;

public interface ModelInterface {
    public Result solve(ModelSolutionInterface initialSolution, JobDataInstance data);
}