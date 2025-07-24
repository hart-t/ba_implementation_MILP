package logic;

import interfaces.HeuristicInterface;
import interfaces.ModelInterface;
import io.JobDataInstance;
import io.Result;
import enums.HeuristicType;
import enums.PriorityRuleType;
import enums.ModelType;

import java.util.*;

public class IntegratedApproach {
    
    private HeuristicInterface openingHeuristic = null;
    private List<HeuristicInterface> improvementHeuristics = new ArrayList<>();
    private WarmstartSolver solver;
    private boolean usesHeuristics = false;
    
    public IntegratedApproach(List<String> heuristicConfigs, String modelConfig) {
        List<HeuristicInterface> heuristics = createHeuristics(heuristicConfigs);
        ModelInterface model = createModel(modelConfig);
        
        if (!heuristics.isEmpty()) {
            this.openingHeuristic = heuristics.get(0);
            this.improvementHeuristics = heuristics.subList(1, heuristics.size());
            this.usesHeuristics = true;
        }
        this.solver = new WarmstartSolver(model);   
    }
    
    private List<HeuristicInterface> createHeuristics(List<String> configs) {
        List<HeuristicInterface> heuristics = new ArrayList<>();
        
        for (String config : configs) {
            String[] parts = config.split("-");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid heuristic config: " + config + 
                    ". Expected format: 'HEURISTIC-PRIORITYRULE' (e.g., 'SGS-SPT')");
            }
            
            HeuristicType heuristicType = HeuristicType.fromCode(parts[0]);
            PriorityRuleType priorityRule = PriorityRuleType.fromCode(parts[1]);
            
            heuristics.add(heuristicType.createHeuristic(priorityRule));
            
            System.out.println("Added heuristic: " + heuristicType.getDescription() + 
                             " with " + priorityRule.getDescription());
        }
        
        return heuristics;
    }
    
    private ModelInterface createModel(String config) {
        ModelType modelType = ModelType.fromCode(config);
        System.out.println("Using model: " + modelType.getDescription());
        return modelType.createModel();
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
