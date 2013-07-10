import org.jetbrains.annotations.*;
import org.jetbrains.kannotator.runtime.annotations.*;
import org.jetbrains.kannotator.runtime.annotations.Mutable;
import org.jetbrains.kannotator.runtime.annotations.ReadOnly;

public class A<T, G> {

    @NotNull
    Integer i;

    @Mutable
    T tfield;

    @NotNull
    A<T, Short> ouch;


    @NotNull
    int f() {
        return 1;
    }

    int g(@NotNull @Mutable Integer f, @Nullable @ReadOnly int p1) {
        return f;
    }

    @Nullable
    Integer c(@Nullable @Mutable T t, @NotNull @ReadOnly G g) {
        return 10;
    }
}
