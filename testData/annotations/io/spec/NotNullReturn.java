package annotations.io.spec;

import org.jetbrains.annotations.NotNull;

public class NotNullReturn {
    public @NotNull Object m1(Object arg1, Object[] arg2, Object[] arg3, int arg4) {
        return "";
    }

    public static @NotNull String m2(String arg1, Object arg2, Object[] arg3, Object arg4) {
        return "";
    }
}
