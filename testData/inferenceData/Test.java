package inferenceData;

public class Test {
    public Object testNull() {
        return null;
    }

    public Object testNullOrObject() {
        if ("abc".getBytes().length == 0)
            return new Object();
        else
            return null;
    }

    public void testNotNullParameter(String a) {
        a.getBytes();
    }

    public void testInvocationOnCheckedParameter(String a) {
        if (a != null) {
            a.getBytes();
        }
    }

    public void testIncompatibleChecks(String a) {
        if (a != null && a == null) {
            a.getBytes();
        }
    }

    public void testInvocationOnNullParameter(String a) {
        if (a == null) {
            a.getBytes();
        }
    }
}
