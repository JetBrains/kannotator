package fieldDependency.simple;

public class Simple {
    private static String PREFIX = staticInit();
    private static Object PREFIX_OBJECT = new Object();

    private final String finalField;
    private String nonFinalField;

    public Simple(String param) {
        finalField = param;
        nonFinalField = param;
    }

    public Simple() {
        finalField = "Hello";
    }

    public static String staticInit() {
        return "INIT";
    }

    public String getFinalField() {
        return finalField;
    }

    public String getNonFinalField() {
        return nonFinalField;
    }

    public String setNonFinalField(String param) {
        return nonFinalField = param;
    }
}
