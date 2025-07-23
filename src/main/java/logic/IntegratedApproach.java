package logic;

import interfaces.HeuristicInterface;
import io.JobDataInstance;
import io.Result;

import java.util.*;

public class IntegratedApproach {
    
    private HeuristicInterface openingHeuristic = null;
    private List<HeuristicInterface> improvementHeuristics = new ArrayList<>();
    private WarmstartSolver solver;
    private boolean usesHeuristics = false;
    
    public IntegratedApproach(List<HeuristicInterface> heuristics, WarmstartSolver solver) {
        if (!heuristics.isEmpty()) {
            this.openingHeuristic = heuristics.get(0);
            this.improvementHeuristics = heuristics.subList(1, heuristics.size());
            this.usesHeuristics = true;
        }
        this.solver = solver;   
    }
    
    
    public Result solve(JobDataInstance data) {
        Map<Integer, Integer> startTimes = new HashMap<>();
        if (usesHeuristics) {
            startTimes = openingHeuristic.determineStartTimes(data);        
        }

        if (!improvementHeuristics.isEmpty()) {
            for (HeuristicInterface heuristic : improvementHeuristics) {
                startTimes = heuristic.determineStartTimes(data, startTimes);
            }
        }

        return solver.solve(startTimes, data);
    }
}
