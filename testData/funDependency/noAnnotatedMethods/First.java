package funDependency.noAnnotatedMethods;

public class First {
    public First(Object a) {
    }

    public First(int a) {
    }

    public void first() {

    }

    public int second() {
        return 1;
    }

    public void third(int a, byte b, double c, float d, short e, long f) {
    }

    public void fourth(int a, Object some) {
    }

    public Object fifth(int a) {
        return null;
    }
}
