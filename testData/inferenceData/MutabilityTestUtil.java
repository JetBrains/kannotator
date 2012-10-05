package inferenceData;

import inferenceData.annotations.Mutable;

import java.util.Collection;
import java.util.List;

public class MutabilityTestUtil {
    public void processMutable(@Mutable List<String> list) {
    }

    public static void processReadableAndMutable(List<String> list, @Mutable Collection<String> collection) {
    }
}
