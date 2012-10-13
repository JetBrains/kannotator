package inferenceData.propagation.down;

import inferenceData.annotations.ExpectNotNull;
import inferenceData.annotations.ExpectNullable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LinearHierarchyCovariantReturn {
    public interface A {
        @Nullable
        Object test(@NotNull Object o);
    }

    public interface B extends A {
        @NotNull
        Object test(@Nullable @ExpectNotNull Object o);
    }

    public interface C extends B {
        @ExpectNotNull
        Object test(@Nullable @ExpectNotNull Object o);
    }
}
