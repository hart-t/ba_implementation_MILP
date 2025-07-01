package logic;

import java.util.Map;

import interfaces.CompletionModelInterface;
import interfaces.ModelSolutionInterface;
import io.JobDataInstance;
import io.Result;

public class WarmstartSolver {
    
    private CompletionModelInterface completionMethod;
    private ModelSolutionInterface model;
    
    public WarmstartSolver(CompletionModelInterface completionMethod, ModelSolutionInterface model) {
        this.completionMethod = completionMethod;
        this.model = model;
    }

    public Result solve(Map<Integer, Integer> startTimes, JobDataInstance data) {
        return new Result();
    }
}
