package logic;

import interfaces.HeuristicInterface;
import interfaces.ModelInterface;
import io.JobDataInstance;
import io.Result;
import io.ScheduleResult;
import enums.*;

import java.util.*;

public class IntegratedApproach {
    private List<HeuristicInterface> heuristics = new ArrayList<>();
    private WarmstartSolver solver;
    private int maxRuntime;

    public IntegratedApproach(List<String> heuristicConfigs, String modelConfig, WarmstartStrategy strategy) {
        this.heuristics = createHeuristics(heuristicConfigs);
        ModelInterface model = createModel(modelConfig);
        this.solver = new WarmstartSolver(model, maxRuntime, strategy);
    }
    
    private List<HeuristicInterface> createHeuristics(List<String> configs) {
        List<HeuristicInterface> heuristics = new ArrayList<>();
        
        for (String config : configs) {
            String[] parts = config.split("-");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid heuristic config: " + config + 
                    ". Expected format: 'HEURISTIC-PRIORITYRULE-SAMPLINGTYPE' (e.g., 'SSGS-SPT-NS', SSGS-SPT-RS_50)");
            }
            
            HeuristicType heuristicType = HeuristicType.fromCode(parts[0]);

            //TODO make it better
            if (heuristicType.equals(HeuristicType.GA)) {
                heuristics.add(heuristicType.createHeuristic(PriorityRuleType.MJS, SamplingType.NS));
            } else {
                PriorityRuleType priorityRule = PriorityRuleType.fromCode(parts[1]);

                String[] samplingConfigParts = parts[2].split("_");
                SamplingType samplingType = SamplingType.fromCode(samplingConfigParts[0]);

                String log = "Added heuristic: " + heuristicType.getDescription() + 
                    " with " + priorityRule.getDescription() + " with " + samplingType.getDescription();

                if (samplingType.equals(SamplingType.NS)) {
                    heuristics.add(heuristicType.createHeuristic(priorityRule, samplingType));
                } else {
                    for (int i = 0; i < Integer.parseInt(samplingConfigParts[1]); i++) {
                        heuristics.add(heuristicType.createHeuristic(priorityRule, samplingType));
                    }
                    log += " with " + samplingConfigParts[1] + " tries";
                }
                System.out.println(log);
            }
        }
        
        return heuristics;
    }
    
    private ModelInterface createModel(String config) {
        String[] parts = config.split("-");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid model config: " + config + 
                    ". Expected format: 'MODEL-TIME' (e.g., 'DISC-30', where '30' is the max runtime in seconds)");
            }
        ModelType modelType = ModelType.fromCode(parts[0]);
        this.maxRuntime = Integer.parseInt(parts[1]);
        System.out.println("Using model: " + modelType.getDescription() + " with max runtime: " + maxRuntime + " seconds");
        return modelType.createModel();
    }

    public Result solve(JobDataInstance data) {
        ScheduleResult scheduleResult = new ScheduleResult(new HashSet<>(), new ArrayList<Map<Integer,Integer>>());
        Result result;
        if (!heuristics.isEmpty()) {
            long startTime = System.nanoTime();
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

            long timeComputingHeuristicStartTimes = System.nanoTime() - startTime;
            scheduleResult.setTimeComputingHeuristicStartTimes(timeComputingHeuristicStartTimes);
            result = solver.solve(data, scheduleResult);
        } else {
            result = solver.solve(data, scheduleResult);
        }
        result.setSamplingMethod("SomeMethod");
        result.setSamplingSize("SomeSize");
        result.printResult();
        return result;
    }
}
