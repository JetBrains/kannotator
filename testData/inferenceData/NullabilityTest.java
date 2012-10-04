package inferenceData;

import inferenceData.annotations.ExpectNotNull;
import inferenceData.annotations.ExpectNullable;

import java.util.Collection;

public class NullabilityTest {
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

    public void testInvocationOnCheckedParameter(@ExpectNullable String a) {
        if (a != null) {
            a.getBytes();
        }
    }
    
    public void testIncompatibleChecks(@ExpectNullable String a) {
        if (a != null && a == null) {
            a.getBytes();
        }
    }

    public void testInvocationOnNullParameter(@ExpectNullable String a) {
        if (a == null) {
            a.getBytes();
        }
    }

    public void testNullableParameter(@ExpectNullable String a) {
        if (a == null) return;
        System.out.println(a);
    }

    public void testSenselessIsNullCheck(@ExpectNotNull String a) {
        a.getBytes();
        if (a == null) return;
    }

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

    @ExpectNotNull
    public Class<?> testClassLiteral() {
        return Integer.class;
    }

    public void testNotNullIfNullCheckThrowsException(@ExpectNotNull String a) {
        if (a == null) throw new NullPointerException();
    }

    @ExpectNotNull
    public String testAssertAfterReturn(@ExpectNotNull String a, boolean condition) {
        if (condition) return a;
        a.getBytes();
        return "";
    }

    public void testGetField(@ExpectNotNull NullabilityTest p) {
        Object o = p.field;
    }

    public void testPutField(@ExpectNotNull NullabilityTest p) {
        p.field = null;
    }

    public void testArrayLength(@ExpectNotNull int[] o1, @ExpectNotNull Object[] o2) {
        System.out.println(o1.length + o2.length);
    }

    public void testThrowParameter(@ExpectNotNull AssertionError e) {
        throw e;
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
