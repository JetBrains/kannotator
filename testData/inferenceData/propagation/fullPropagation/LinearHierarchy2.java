package inferenceData.propagation.fullPropagation;

import inferenceData.annotations.ExpectNotNull;
import inferenceData.annotations.ExpectNullable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LinearHierarchy2 {
    public interface A {
        @ExpectNullable
        Object m(Object x, @ExpectNotNull Object y, @ExpectNullable Object z);
    }

    public interface B extends A {
        @Nullable
        Object m(Object x, @Nullable @ExpectNotNull Object y, @ExpectNullable Object z);
    }

    public interface C extends B {
        @NotNull
        Object m(Object x, @NotNull Object y, @Nullable Object z);
    }
}
