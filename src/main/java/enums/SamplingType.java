package enums;

import interfaces.SamplingTypeInterface;
import samplingTypes.*;

public enum SamplingType {
    NS("NS", "No Sampling", NoSampling::new),
    RS("RS", "Random Sampling", RandomSampling::new),
    BRS("BRS", "Biased Random Sampling", BiasedRandomSampling::new),
    RBRS("RBRS", "Regret Based Biased Random Sampling", RegretBasedBiasedRandomSampling::new);

    private final String code;
    private final String description;
    private final SamplingTypeSupplier supplier;

    SamplingType(String code, String description, SamplingTypeSupplier supplier) {
        this.code = code;
        this.description = description;
        this.supplier = supplier;
    }
    
    public String getCode() { return code; }
    public String getDescription() { return description; }
    public SamplingTypeInterface createSamplingType() { return supplier.get(); }

    public static SamplingType fromCode(String code) {
        for (SamplingType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown sampling type code: " + code);
    }
    
    @FunctionalInterface
    public interface SamplingTypeSupplier {
        SamplingTypeInterface get();
    }
}
