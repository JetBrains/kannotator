package inferenceData.propagation.down;

import inferenceData.annotations.ExpectNotNull;
import inferenceData.annotations.ExpectNullable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LinearHierarchyAlterChildren {
    public interface A {
        @NotNull
        Object test(@NotNull Object o);
    }

    public interface B extends A {
        @ExpectNotNull
        Object test(@ExpectNotNull Object o);
    }

    public interface C extends B {
        @ExpectNotNull
        Object test(@Nullable @ExpectNotNull Object o);
    }

    public interface D extends C {
        @ExpectNotNull
        Object test(@Nullable @ExpectNotNull Object o);
    }
}
