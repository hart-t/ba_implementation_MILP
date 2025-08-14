package enums;

import interfaces.HeuristicInterface;
import heuristics.*;

public enum HeuristicType {
    SSGS("SSGS", "Serial Schedule Generation Scheme", true),
    GA("GA", "Genetic Algorithm", false),
    // Add improvement heuristics here with false flag
    ;
    
    private final String code;
    private final String description;
    private final boolean isOpeningHeuristic;
    
    HeuristicType(String code, String description, boolean isOpeningHeuristic) {
        this.code = code;
        this.description = description;
        this.isOpeningHeuristic = isOpeningHeuristic;
    }
    
    public String getCode() { return code; }
    public String getDescription() { return description; }
    public boolean isOpeningHeuristic() { return isOpeningHeuristic; }
    
    public HeuristicInterface createHeuristic(PriorityRuleType priorityRule, SamplingType samplingType) {
        switch (this) {
            case SSGS:
                return new HeuristicSerialSGS(priorityRule.createRule(), samplingType.createSamplingType());
            case GA:
                return new HeuristicGeneticAlgorithm();
            default:
                throw new UnsupportedOperationException("Heuristic not implemented: " + this);
        }
    }
    
    public static HeuristicType fromCode(String code) {
        for (HeuristicType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown heuristic code: " + code);
    }
}
