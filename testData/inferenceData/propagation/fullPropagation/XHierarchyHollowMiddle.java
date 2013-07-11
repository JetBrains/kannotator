package inferenceData.propagation.fullPropagation;

import inferenceData.annotations.ExpectNotNull;
import inferenceData.annotations.ExpectNullable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class XHierarchyHollowMiddle {

    public interface Top1 {
        @ExpectNullable
        Object m(@ExpectNotNull Object x, Object y, @NotNull Object z);
    }

    public interface Top2 {
        @ExpectNullable
        Object m(@NotNull Object x, Object y, @ExpectNotNull Object z);
    }

    public interface Middle extends Top1, Top2 {
        @ExpectNullable
        Object m(@ExpectNotNull Object x, Object y, @ExpectNotNull Object z);

        void dummy(); // Dummy method to avoid interpreting this interface as SAM -> propagation errors on loading
    }

    public interface Leaf1 extends Middle {
        @Nullable
        Object m(@Nullable @ExpectNotNull Object x, Object y, @ExpectNotNull Object z);
    }

    public interface Leaf2 extends Middle {
        @NotNull
        Object m(@ExpectNotNull Object x, Object y, @Nullable @ExpectNotNull Object z);
    }

}
