package funDependency.funInDifferentClasses;

public class First {
    private Second second = new Second();

    public void test() {
        second.foo();
    }
}
