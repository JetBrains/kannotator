package annotations.io.spec;

import org.jetbrains.annotations.NotNull;

public class NotNullArgs {
    public void m1(Object arg1, @NotNull Object[] arg2, @NotNull Object[] arg3, int arg4) {

    }

    public static void m2(@NotNull String arg1, @NotNull Object arg2, Object[] arg3, @NotNull Object arg4) {

    }
}
