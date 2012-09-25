package funDependency.recursiveFun;

public class Second {
    public Object bar() {
        return new First().foo();
    }
}

