package inferenceData.propagation.fullPropagation;

import inferenceData.annotations.ExpectNotNull;
import inferenceData.annotations.ExpectNullable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class YHierarchy {

    public interface A {
        @ExpectNullable
        Object m(@ExpectNullable Object x, @ExpectNullable Object y, @NotNull Object z);
    }

    public interface A1 {
        @ExpectNullable
        Object m(@Nullable Object x, @Nullable Object y, @Nullable @ExpectNotNull Object z);
    }

    public interface B extends A, A1 {
        @Nullable
        Object m(@ExpectNullable Object x, @Nullable Object y, @ExpectNotNull Object z);

        void dummy(); // Dummy method to avoid interpreting this interface as SAM -> propagation errors on loading
    }

    public interface C extends B {
        Object m(@ExpectNullable Object x, @ExpectNullable Object y, @ExpectNotNull Object z);
    }

}
