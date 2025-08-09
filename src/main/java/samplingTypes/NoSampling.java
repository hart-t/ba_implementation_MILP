package samplingTypes;

import enums.SamplingType;
import interfaces.*;

public class NoSampling implements SamplingTypeInterface {
    private SamplingType samplingType = SamplingType.NS;

    @Override
    public SamplingType getSamplingType() {
        return samplingType;
    }
}
