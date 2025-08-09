package interfaces;

import enums.SamplingType;

public interface SamplingTypeInterface {
    /**
     * Returns the type of sampling used in the priority rule.
     *
     * @return a string representing the sampling type
     */
    SamplingType getSamplingType();
}
