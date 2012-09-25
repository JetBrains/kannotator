package funDependency.recursiveFun;

public class First {
    public Object foo() {
       new Second().bar();
       return foo();
    }
}
