package logic;

import interfaces.HeuristicInterface;
import io.JobDataInstance;
import io.Result;
import java.util.*;

public class IntegratedApproach {
    
    private HeuristicInterface openingHeuristic;
    private List<HeuristicInterface> improvementHeuristics;
    private WarmstartSolver solver;
    
    public IntegratedApproach(List<HeuristicInterface> heuristics, WarmstartSolver solver) {
        this.openingHeuristic = heuristics.get(0);
        this.improvementHeuristics = heuristics.subList(1, heuristics.size());
        this.solver = solver;   
    }
    
    
    public Result solve(JobDataInstance data) {
        Map<Integer, Integer> startTimes = openingHeuristic.determineStartTimes(data);

        if (!improvementHeuristics.isEmpty()) {
            for (HeuristicInterface heuristic : improvementHeuristics) {
                startTimes = heuristic.determineStartTimes(data);
            }
        }

        solver.solve(startTimes, data);



        return null;
    }
}
