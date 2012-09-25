package funDependency.recursiveFun;

public class First {
    public void foo() {
       new Second().bar();
       foo();
    }
}
