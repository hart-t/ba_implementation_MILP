package interfaces;

import io.JobDataInstance;
import java.util.*;

public interface CompletionModelInterface {
    public ModelSolutionInterface completeSolution(Map<Integer, Integer> startTimes, JobDataInstance data);
}
