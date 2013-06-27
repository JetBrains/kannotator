package inferenceData.propagation.fullPropagation;

import inferenceData.annotations.ExpectNotNull;
import inferenceData.annotations.ExpectNullable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class XHierarchyAnnotatedMiddle {
    public interface Top1 {
        @ExpectNotNull
        Object m(@ExpectNullable Object x, @ExpectNotNull Object y, Object z);
    }

    public interface Top2 {
        @Nullable
        Object m(@ExpectNullable Object x, @ExpectNotNull Object y, Object z);
    }

    public interface Middle extends Top1, Top2 {
        @NotNull
        Object m(@Nullable Object x, @NotNull Object y, Object z);

        void dummy(); // Dummy method to avoid interpreting this interface as SAM -> propagation errors on loading
    }

    public interface Leaf1 extends Middle {
        Object m(@ExpectNullable Object x, @ExpectNotNull Object y, Object z);
    }

    public interface Leaf2 extends Middle {
        Object m(@ExpectNullable Object x, @ExpectNotNull Object y, Object z);
    }

}
