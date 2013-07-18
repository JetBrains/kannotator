package dependencies.a;

import dependencies.b.B;

public class A {
    public String inA(String s) {
        return new B().inB(s);
    }

    public static String staticInA(String s) {
        return B.staticInB(s);
    }
}
