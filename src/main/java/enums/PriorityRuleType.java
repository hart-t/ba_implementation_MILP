package enums;

import interfaces.PriorityRuleInterface;
import priorityRules.*;

public enum PriorityRuleType {
    SPT("SPT", "Shortest Processing Time", ShortestProcessingTimeRule::new),
    MRU("MRU", "Most Resource Usage", MostResourceUsageRule::new),
    RPW("RPW", "Rank Positional Weight", GreatestRankPositionalWeightRule::new);
    
    private final String code;
    private final String description;
    private final PriorityRuleSupplier supplier;
    
    PriorityRuleType(String code, String description, PriorityRuleSupplier supplier) {
        this.code = code;
        this.description = description;
        this.supplier = supplier;
    }
    
    public String getCode() { return code; }
    public String getDescription() { return description; }
    public PriorityRuleInterface createRule() { return supplier.get(); }
    
    public static PriorityRuleType fromCode(String code) {
        for (PriorityRuleType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown priority rule code: " + code);
    }
    
    @FunctionalInterface
    public interface PriorityRuleSupplier {
        PriorityRuleInterface get();
    }
}
