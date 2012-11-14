package inferenceData.propagation.conflicts;

import inferenceData.annotations.ExpectNotNull;
import inferenceData.annotations.ExpectNullable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AHierarchy {

    public interface A {
        @NotNull @ExpectNullable
        Object m(@Nullable @ExpectNotNull Object x);
    }

    public interface B extends A {
        @ExpectNullable
        Object m(Object x);
    }

    public interface C extends B {
        @Nullable
        Object m(Object x);
    }

    public interface B1 extends A {
        Object m(Object x);
    }

    public interface C1 extends B1 {
        Object m(@NotNull Object x);
    }

}
