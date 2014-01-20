package annotations.io.spec;

import org.jetbrains.annotations.Nullable;

public class NullableReturn {
    public @Nullable
    Object m1(Object arg1, Object[] arg2, Object[] arg3, int arg4) {
        return "";
    }

    public static @Nullable String m2(String arg1, Object arg2, Object[] arg3, Object arg4) {
        return "";
    }
}
