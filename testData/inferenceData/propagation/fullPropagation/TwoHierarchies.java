package inferenceData.propagation.fullPropagation;

import inferenceData.annotations.ExpectNotNull;
import inferenceData.annotations.ExpectNullable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TwoHierarchies {
    public interface A1 {
        void m(@NotNull @ExpectNotNull Object o);
    }

    public interface A2 extends A1 {
        void m(@NotNull @ExpectNotNull Object o);
    }

    public interface B1 {
        void m(@Nullable @ExpectNullable Object o);
    }

    public interface B2 extends B1 {
        void m(@Nullable @ExpectNullable Object o);
    }
}
