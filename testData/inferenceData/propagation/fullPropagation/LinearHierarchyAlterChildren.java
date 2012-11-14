package inferenceData.propagation.fullPropagation;

import inferenceData.annotations.ExpectNotNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LinearHierarchyAlterChildren {
    public interface A {
        @NotNull
        Object test(@NotNull Object o);
    }

    public interface B extends A {
        Object test(@ExpectNotNull Object o);
    }

    public interface C extends B {
        Object test(@Nullable @ExpectNotNull Object o);
    }

    public interface D extends C {
        Object test(@Nullable @ExpectNotNull Object o);
    }
}
