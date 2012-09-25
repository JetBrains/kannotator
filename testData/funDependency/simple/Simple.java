package funDependency.simple;

public class Simple {
    public Object foo() {
        return bar();
    }

    public Object bar() {
        System.out.println();
        return null;
    }
}

