package inferenceData;

import com.sun.istack.internal.NotNull;
import inferenceData.annotations.ExpectNotNull;
import inferenceData.annotations.ExpectNullable;
import org.jetbrains.annotations.Nullable;

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

    @ExpectNotNull
    public final static Class<String> stringClass = String.class;

    @ExpectNullable
    public final Object nullFinalField = null;

    @ExpectNotNull
    public final Object newObjectFinalField = new Object();

    @ExpectNotNull
    public final String constantStringFinalField = "HelloConstant";

    // TODO: @ExpectNotNull
    public final Integer constantIntegerFinalField = 12;

    @ExpectNotNull
    public final Object methodInitFinalField = initFinalField();

    @NotNull
    Object initFinalField() {
        return new Object();
    }

    @ExpectNullable
    public final Object nullableInConstructorInitFinalField;

    @ExpectNullable
    public final Object fromConstructorParameterFinalField;

    @ExpectNotNull
    public final Object fromMethodInConstructorFinalField;

    @ExpectNullable
    public final Object differentAnnotationsFromDifferentConstructors;

    @ExpectNotNull
    public Object nullableFromCallingMethodOnValue;

    @ExpectNullable
    public Object notUsedNonFinalField;

    public void setNullableFromCallingMethodOnValue(Object param) {
        nullableFromCallingMethodOnValue = param;
        nullableFromCallingMethodOnValue.toString();
    }

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

    NullabilityFieldsInferenceTestClass(Object param, @Nullable Object param1) {
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
