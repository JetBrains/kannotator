package inferenceData.propagation;

import inferenceData.annotations.ExpectNotNull;
import inferenceData.annotations.ExpectNullable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Conflicts {
    public static class Base {
        @ExpectNullable @NotNull
        Object test1(@ExpectNotNull @Nullable Object bar) {
            return null;
        }

        @Nullable
        Object test2(@NotNull Object bar) {
            return null;
        }

        @Nullable
        Object test3(@Nullable Object p0, Object p1) {
            return null;
        }

        @NotNull
        Object test4(@NotNull Object p0, Object p1) {
            return null;
        }

        Object test5(@NotNull Object p0) { return null; }
    }

    public static class Child extends Base {
        @Nullable
        Object test1(@NotNull Object bar) {
            return null;
        }

        @NotNull
        Object test2(@Nullable Object bar) {
            return null;
        }

        @Nullable
        Object test3(@Nullable Object p0, Object p1) {
            return null;
        }

        @NotNull
        Object test4(@NotNull Object p0, Object p1) {
            return null;
        }

        Object test5(@NotNull Object p0) { return null; }
    }
}

