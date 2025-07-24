package enums;

import interfaces.HeuristicInterface;
import heuristics.*;

public enum HeuristicType {
    SGS("SGS", "Serial Schedule Generation Scheme", true),
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
    
    public HeuristicInterface createHeuristic(PriorityRuleType priorityRule) {
        switch (this) {
            case SGS:
                return new HeuristicSerialSGS(priorityRule.createRule());
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
