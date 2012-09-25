package funDependency.funInDifferentClasses;

public class First {
    private Second second = new Second();

    public Object test() {
        return second.foo();
    }
}
