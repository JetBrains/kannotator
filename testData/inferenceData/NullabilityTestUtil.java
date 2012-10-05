package inferenceData;

import org.jetbrains.annotations.NotNull;

public class NullabilityTestUtil {
    public void assertNotNull(@NotNull Object o) {
    }

    public void nullableParameter(Object o) {
    }

    public void assertSecondNotNull(Object o1, @NotNull Object o2) {
    }

    public static void staticAssertNotNull(@NotNull Object o) {
    }
}
