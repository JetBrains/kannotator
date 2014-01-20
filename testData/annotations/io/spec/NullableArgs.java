package annotations.io.spec;

import org.jetbrains.annotations.Nullable;

public class NullableArgs {
    public void m1(Object arg1, @Nullable Object[] arg2, @Nullable Object[] arg3, int arg4) {

    }

    public static void m2(@Nullable String arg1, @Nullable Object arg2, Object[] arg3, @Nullable Object arg4) {

    }
}
