package interpreter._static;

public class Test {
    public static Object foo(String bar) {
        return bar;
    }

    public static Object foo() {
        return "";
    }

    public static void fooVoid(String bar) {
        foo(bar);
    }
}
