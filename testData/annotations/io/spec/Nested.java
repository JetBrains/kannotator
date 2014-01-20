package annotations.io.spec;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Nested {
    public static class A {
        public @Nullable Object ma(@NotNull Object o) {
            return null;
        }
    }
}
