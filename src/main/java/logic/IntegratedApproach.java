package logic;

import interfaces.HeuristicInterface;
import interfaces.ModelInterface;
import io.JobDataInstance;
import io.Result;
import io.ScheduleResult;
import enums.HeuristicType;
import enums.PriorityRuleType;
import enums.ModelType;

import java.util.*;

public class IntegratedApproach {
    private List<HeuristicInterface> heuristics = new ArrayList<>();
    private WarmstartSolver solver;

    public IntegratedApproach(List<String> heuristicConfigs, String modelConfig) {
        this.heuristics = createHeuristics(heuristicConfigs);
        ModelInterface model = createModel(modelConfig);
        this.solver = new WarmstartSolver(model);   
    }
    
    private List<HeuristicInterface> createHeuristics(List<String> configs) {
        List<HeuristicInterface> heuristics = new ArrayList<>();
        
        for (String config : configs) {
            String[] parts = config.split("-");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid heuristic config: " + config + 
                    ". Expected format: 'HEURISTIC-PRIORITYRULE' (e.g., 'SSGS-SPT')");
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
    
    public void solve(JobDataInstance data) {
        ScheduleResult scheduleResult = new ScheduleResult(new ArrayList<>(), new HashMap<>());
        Result result;
        if (!heuristics.isEmpty()) {
            List<HeuristicInterface> openingHeuristics = new ArrayList<>();
            List<HeuristicInterface> improvementHeuristics = new ArrayList<>();

            for (HeuristicInterface heuristic : heuristics) {
                if (heuristic.isOpeningHeuristic()) {
                    openingHeuristics.add(heuristic);
                } else {
                    improvementHeuristics.add(heuristic);
                }
            }

            if (openingHeuristics.isEmpty()) {
                throw new IllegalStateException("No opening heuristics configured. " +
                    "At least one heuristic must be an opening heuristic.");
            }

            for (HeuristicInterface heuristic : openingHeuristics) {
                scheduleResult = heuristic.determineScheduleResult(data, scheduleResult);
            }
            for (HeuristicInterface heuristic : improvementHeuristics) {
                scheduleResult = heuristic.determineScheduleResult(data, scheduleResult);
            }
            result = solver.solve(data, scheduleResult);
        } else {
            result = solver.solve(data, scheduleResult);
        }
        //TODO write in file
        result.printResult();
    }
}
