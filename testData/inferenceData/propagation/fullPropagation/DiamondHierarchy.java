package inferenceData.propagation.fullPropagation;

import inferenceData.annotations.ExpectNotNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DiamondHierarchy {

    public interface Top {
        @ExpectNotNull
        Object m(@ExpectNotNull Object x, Object y, @ExpectNotNull Object z);
    }

    public interface A extends Top {
        @ExpectNotNull
        Object m(@Nullable @ExpectNotNull Object x, Object y, @ExpectNotNull Object z);
    }

    public interface A1 extends Top {
        @ExpectNotNull
        Object m(@ExpectNotNull Object x, Object y, @ExpectNotNull Object z);
    }

    public interface B extends A, A1 {
        @NotNull
        Object m(@ExpectNotNull Object x, Object y, @ExpectNotNull Object z);
    }

    public interface C extends B {
        @ExpectNotNull
        Object m(@NotNull Object x, Object y, @NotNull Object z);
    }

}
