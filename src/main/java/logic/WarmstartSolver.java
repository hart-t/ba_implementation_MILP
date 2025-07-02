package logic;

import java.util.Map;

import interfaces.CompletionMethodInterface;
import interfaces.ModelSolutionInterface;
import io.JobDataInstance;
import io.Result;

public class WarmstartSolver {
    
    private CompletionMethodInterface completionMethod;
    private ModelSolutionInterface model;
    
    public WarmstartSolver(CompletionMethodInterface completionMethod, ModelSolutionInterface model) {
        this.completionMethod = completionMethod;
        this.model = model;
    }
}
