package annotations.io.spec;

import org.jetbrains.annotations.NotNull;

public class NotNullFields {
    public @NotNull int[][] arrayField = new int[0][0];
    public @NotNull Object objField = new Object();
}
