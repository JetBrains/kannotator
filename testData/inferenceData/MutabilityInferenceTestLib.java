package inferenceData;

import org.jetbrains.kannotator.runtime.annotations.Mutable;

import java.util.Collection;
import java.util.List;

public class MutabilityInferenceTestLib {
    public void processMutable(@Mutable List<String> list) {
    }

    public static void processReadableAndMutable(List<String> list, @Mutable Collection<String> collection) {
    }
}
