package inferenceData;

import inferenceData.annotations.ExpectNotNull;
import inferenceData.annotations.ExpectNullable;

public class NullabilityFieldsInferenceTestClass {
    @ExpectNotNull
    public final static String STRING_NOT_NULL_FIELD = "String";

    @ExpectNullable
    public final static String STRING_NULL_FIELD = null;

    @ExpectNotNull
    public final static String FROM_PREVIOUS_FIELD = STRING_NOT_NULL_FIELD + "HELLO";

    @ExpectNotNull
    public final static Object NEW_OBJECT_FIELD = new Object();

    public final static int INTEGER_FIELD = 12;

    public final static double DOUBLE_FIELD = 12.5;

    // @ExpectNullable
    public final Object nullFinalField = null;

    // @ExpectNotNull
    public final Object newObjectFinalField = new Object();

    // @ExpectNotNull
    public final String constantStringFinalField = "HelloConstant";

    // @ExpectNotNull
    public final Integer constantIntegerFinalField = 12;

    // @ExpectNotNull
    public final Object methodInitFinalField = initFinalField();

    Object initFinalField() {
        return new Object();
    }

    // @ExpectNullable
    public final Object nullableInConstructorInitFinalField;

    // @ExpectNullable
    public final Object fromConstructorParameterFinalField;

    // @ExpectNotNull
    public final Object fromMethodInConstructorFinalField;

    // @ExpectNullable
    public final Object differentAnnotationsFromDifferentConstructors;

    NullabilityFieldsInferenceTestClass(Object param) {
        if (param == null) {
            nullableInConstructorInitFinalField = null;
        } else {
            nullableInConstructorInitFinalField = new Object();
        }
        fromConstructorParameterFinalField = param;
        fromMethodInConstructorFinalField = initFinalField();

        differentAnnotationsFromDifferentConstructors = new Object();
    }

    NullabilityFieldsInferenceTestClass(Object param, Object param1) {
        if (param == null) {
            nullableInConstructorInitFinalField = null;
        } else {
            nullableInConstructorInitFinalField = new Object();
        }
        fromConstructorParameterFinalField = param;
        fromMethodInConstructorFinalField = initFinalField();

        differentAnnotationsFromDifferentConstructors = param1;
    }

    NullabilityFieldsInferenceTestClass() {
        this(null);
    }
}
