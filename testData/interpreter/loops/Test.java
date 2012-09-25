package interpreter.loops;

public class Test {
    static void consume(Object x) {}

    static void test1() {
        int i = 0;
        while (i < 10) i++;
        consume(i);
    }

    static void test2() {
        Object x = null;
        while (x == null) x = "";
        consume(x);
    }

    static void test3(Integer i) {
        Object x = null;
        while (x == null) x = i;
        consume(x);
    }

    void test4(Integer i) {
        Object x = null;
        while (x == null) {
            if (x == i) {
                x = i;
            }
            else {
                x = this;
            }
        }
        consume(x);
    }

    void test5() {
        byte[] hashBytes = {};
        long svuid = 0;
        for (int i = Math.min(hashBytes.length, 8) - 1; i >= 0; i--) {
            svuid = (svuid << 8) | (hashBytes[i] & 0xFF);
        }
    }
}
