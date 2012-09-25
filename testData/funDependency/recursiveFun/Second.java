package funDependency.recursiveFun;

public class Second {
    public void bar() {
        new First().foo();
    }
}

