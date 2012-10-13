package inferenceData.propagation.down;

import inferenceData.annotations.ExpectNotNull;
import inferenceData.annotations.ExpectNullable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LinearHierarchyMultipleAnnotations {
    public interface A {
        @Nullable
        Object test(@NotNull Object o, Object x);
    }

    public interface B extends A {
        @ExpectNullable
        Object test(@ExpectNotNull Object o, @Nullable Object x);
    }

    public interface C extends B {
        @ExpectNullable
        Object test(@ExpectNotNull Object o, @ExpectNullable Object x);
    }
}
