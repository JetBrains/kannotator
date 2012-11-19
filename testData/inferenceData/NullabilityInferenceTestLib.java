package inferenceData;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NullabilityInferenceTestLib {
    public void assertNotNull(@NotNull Object o) {
    }

    public void nullableParameter(Object o) {
    }

    public void assertSecondNotNull(Object o1, @NotNull Object o2) {
    }

    public static void staticAssertNotNull(@NotNull Object o) {
    }

    public static void staticAssertSecondNotNull(Object o1, @NotNull Object o2) {
    }

    @NotNull
    public static Object returnNotNull() {
        return new Object();
    }

    @Nullable
    public static Object returnNullable() {
        return null;
    }

    @NotNull
    public Object notNullField;

    @Nullable
    public static Object nullableStaticField;
}
