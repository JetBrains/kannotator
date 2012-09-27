package inferenceData;

import java.util.Collection;

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

    public Object testReturnInvokeSpecial() {
        return privateMethod();
    }

    public Object testReturnInvokeVirtual() {
        return publicMethod();
    }

    public Object testReturnInvokeStatic() {
        return staticMethod();
    }

    public Object testInvokeInterface(Collection<Object> collection) {
        return collection.iterator();
    }

    public Object testReturnArrayLoad(Object[] arr) {
        return arr[0];
    }

    public int[] testReturnNewIntArray() {
        return new int[1];
    }

    public Object[] testReturnNewObjectArray() {
        return new Object[1];
    }

    public int[][] testReturnNewMultiArray() {
        return new int[1][1];
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

    public void testNullableParameter(String a) {
        if (a == null) return;
        System.out.println(a);
    }

    public void testSenselessNotNullCheck(String a) {
        a.getBytes();
        if (a == null) return;
    }

    public void testInvocationAfterReturn(String a) {
        if (a == null) return;
        a.getBytes();
    }








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
