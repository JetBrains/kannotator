package funDependency.dependOnConstructorBecauseOfFields;

public class DependOnConstructorBecauseOfFields {
    private final String finalField;
    private String nonFinalField;

    public DependOnConstructorBecauseOfFields(String param) {
        finalField = param;
        nonFinalField = param;
    }

    public DependOnConstructorBecauseOfFields() {
        finalField = "Hello";
    }

    public String getFinalField() {
        return finalField;
    }

    public String getNonFinalField() {
        return nonFinalField;
    }
}
