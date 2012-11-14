package inferenceData.propagation.fullPropagation;

import inferenceData.annotations.ExpectNotNull;
import inferenceData.annotations.ExpectNullable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LinearHierarchy {
    public interface A {
        @Nullable
        Object test(@NotNull Object o);
    }

    public interface B extends A {
        @ExpectNullable
        Object test(@ExpectNotNull Object o);
    }

    public interface C extends B {
        @ExpectNullable
        Object test(@ExpectNotNull Object o);
    }
}
