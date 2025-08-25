package enums;

import interfaces.ModelInterface;
import models.*;

public enum ModelType {
    FCT("FCT", "Flow-Based Continuous Time Model", FlowBasedContinuousTimeModel::new),
    DT("DT", "Discrete Time Model", DiscreteTimeModel::new),
    OOE("OOE", "On-Off Event Based Model", OnOffEventBasedModel::new),
    IEE("IEE", "Interval Event Based Model", IntervalEventBasedModel::new),
    SEQ("SEQ", "Sequencing Model", SequencingModel::new);

    private final String code;
    private final String description;
    private final ModelSupplier supplier;
    
    ModelType(String code, String description, ModelSupplier supplier) {
        this.code = code;
        this.description = description;
        this.supplier = supplier;
    }
    
    public String getCode() { return code; }
    public String getDescription() { return description; }
    public ModelInterface createModel() { return supplier.get(); }
    
    public static ModelType fromCode(String code) {
        for (ModelType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown model code: " + code);
    }
    
    @FunctionalInterface
    public interface ModelSupplier {
        ModelInterface get();
    }
}
