package inferenceData.propagation.conflicts;

import inferenceData.annotations.ExpectNotNull;
import inferenceData.annotations.ExpectNullable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class YHierarchy {

    public interface A {
        @NotNull @ExpectNullable
        Object m(@Nullable @ExpectNotNull Object x);
    }

    public interface A1 {
        @NotNull @ExpectNullable
        Object m(@Nullable @ExpectNotNull Object x);
    }

    public interface B extends A, A1 {
        @ExpectNullable
        Object m(Object x);
    }

    public interface C extends B {
        @Nullable
        Object m(@NotNull Object x);
    }

}
