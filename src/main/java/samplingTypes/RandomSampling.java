package samplingTypes;

import interfaces.SamplingTypeInterface;
import enums.SamplingType;

public class RandomSampling implements SamplingTypeInterface {
    private SamplingType samplingType = SamplingType.RS;

    @Override
    public SamplingType getSamplingType() {
        return samplingType;
    }
}
