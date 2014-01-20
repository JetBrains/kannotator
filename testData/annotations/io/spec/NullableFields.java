package annotations.io.spec;

import org.jetbrains.annotations.Nullable;

public class NullableFields {
    public @Nullable
    int[][] nullableArrayField = new int[0][0];
    public @Nullable Object nullableObjField = new Object();
}
