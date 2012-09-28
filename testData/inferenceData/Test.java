package inferenceData;

import inferenceData.annotations.ExpectNotNull;
import inferenceData.annotations.ExpectNullable;

import java.util.Collection;

public class Test {
    @ExpectNullable
    public Object testNull() {
        return null;
    }

    @ExpectNullable
    public Object testNullOrObject() {
        if ("abc".getBytes().length == 0)
            return new Object();
        else
            return null;
    }

    public Object testReturnInvokeSpecial() {
        return privateMethod();
    }

    public Object testReturnInvokeVirtual() {
        return publicMethod();
    }

    public Object testReturnInvokeStatic() {
        return staticMethod();
    }

    public Object testInvokeInterface(@ExpectNotNull Collection<Object> collection) {
        return collection.iterator();
    }

    public Object testReturnArrayLoad(@ExpectNotNull Object[] arr) {
        return arr[0];
    }

    @ExpectNotNull
    public int[] testReturnNewIntArray() {
        return new int[1];
    }

    @ExpectNotNull
    public Object[] testReturnNewObjectArray() {
        return new Object[1];
    }

    @ExpectNotNull
    public int[][] testReturnNewMultiArray() {
        return new int[1][1];
    }

    public Object testReturnField() {
        return field;
    }

    public Object testReturnStaticField() {
        return staticField;
    }

    @ExpectNotNull
    public String testReturnStringConstant() {
        return "foo";
    }

    public void testNotNullParameter(@ExpectNotNull String a) {
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

    public void testNullableParameter(@ExpectNullable String a) {
        if (a == null) return;
        System.out.println(a);
    }

//    public void testSenselessNotNullCheck(String a) {
//        a.getBytes();
//        if (a == null) return;
//    }
//
    public void testInvocationAfterReturn(@ExpectNullable String a) {
        if (a == null) return;
        a.getBytes();
    }

    @ExpectNotNull
    public Object testReturnThis() {
        return this;
    }

    @ExpectNotNull
    public Object testReturnCaughtException() {
        try {
            return new Object();
        } catch (Throwable e) {
            return e;
        }
    }

    @ExpectNotNull
    public String testInstanceofAndReturn(Object a) {
        if (a instanceof String) {
            return (String) a;
        }
        else {
            return "";
        }
    }



    private Object field;
    private static Object staticField;

    private Object privateMethod() {
        return null;
    }

    public Object publicMethod() {
        return null;
    }

    static Object staticMethod() {
        return null;
    }
}
