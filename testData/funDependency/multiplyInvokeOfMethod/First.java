package funDependency.multiplyInvokeOfMethod;

public class First {
    public Object foo() {
        Second s = new Second();
        s.foo();
        return s.foo();
    }
}
