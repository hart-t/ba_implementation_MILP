package samplingTypes;

import enums.SamplingType;
import interfaces.SamplingTypeInterface;

public class BiasedRandomSampling implements SamplingTypeInterface{
    private SamplingType samplingType = SamplingType.BRS;

    @Override
    public SamplingType getSamplingType() {
        return samplingType;
    }
}
