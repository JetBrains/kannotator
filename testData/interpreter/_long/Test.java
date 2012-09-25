package interpreter._long;

public class Test {
    private long longPrivateField;

    public class Listener {
        public void testRunStarted() {
            longPrivateField = 1;
        }
    }
}
