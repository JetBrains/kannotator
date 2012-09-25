package funDependency.simple;

public class Simple {
    public void foo() {
        bar();
    }

    public void bar() {
        System.out.println();
    }
}

