package inferenceData;

import inferenceData.annotations.ExpectNotNull;
import inferenceData.annotations.ExpectNullable;

import java.util.*;

public class NullabilityInferenceTestClass {
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

    @ExpectNullable
    public Object testReturnInvokeSpecial() {
        return privateMethod();
    }

    @ExpectNullable
    public Object testReturnInvokeVirtual() {
        return publicMethod();
    }

    @ExpectNullable
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

    @ExpectNullable
    public Object testReturnField() {
        return field;
    }

    @ExpectNotNull
    public Object testReturnNotNullField() {
        return new NullabilityInferenceTestLib().notNullField;
    }

    public Object testReturnStaticField() {
        return staticField;
    }

    @ExpectNullable
    public Object testReturnNullableStaticField() {
        return NullabilityInferenceTestLib.nullableStaticField;
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

    public void testInvocationOnNullParameter(@ExpectNotNull String a) {
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
    public String testInstanceofAndReturn(@ExpectNullable Object a) {
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

    public String testAssertAfterReturn(String a, boolean condition) {
        if (condition) return a;
        a.getBytes();
        return "";
    }

    public void testGetField(@ExpectNotNull NullabilityInferenceTestClass p) {
        Object o = p.field;
    }

    public void testPutField(@ExpectNotNull NullabilityInferenceTestClass p) {
        p.field = null;
    }

    public void testArrayLength(@ExpectNotNull int[] o1, @ExpectNotNull Object[] o2) {
        System.out.println(o1.length + o2.length);
    }

    public void testThrowParameter(@ExpectNotNull AssertionError e) {
        throw e;
    }

    public void testMonitor(@ExpectNotNull Object o) {
        synchronized (o) {
            System.out.println();
        }
    }

    public void testMonitorValueThroughLocalVariable(@ExpectNotNull Object o) {
        Object local = o;
        local.toString();
    }

    private Object tempField;
    public void testMonitorValueThroughField(@ExpectNotNull Object o) {
        tempField = o;
        tempField.toString();
    }

    public void testArrayLoad(@ExpectNotNull Object[] objectArray,
                              @ExpectNotNull byte[] byteArray,
                              @ExpectNotNull boolean[] booleanArray,
                              @ExpectNotNull int[] intArray,
                              @ExpectNotNull float[] floatArray,
                              @ExpectNotNull double[] doubleArray,
                              @ExpectNotNull short[] shortArray,
                              @ExpectNotNull long[] longArray,
                              @ExpectNotNull char[] charArray) {
        System.out.println(objectArray[0]);
        System.out.println(byteArray[0]);
        System.out.println(booleanArray[0]);
        System.out.println(intArray[0]);
        System.out.println(floatArray[0]);
        System.out.println(doubleArray[0]);
        System.out.println(shortArray[0]);
        System.out.println(longArray[0]);
        System.out.println(charArray[0]);
    }

    public void testArrayStore(@ExpectNotNull Object[] objectArray,
                               @ExpectNotNull byte[] byteArray,
                               @ExpectNotNull boolean[] booleanArray,
                               @ExpectNotNull int[] intArray,
                               @ExpectNotNull float[] floatArray,
                               @ExpectNotNull double[] doubleArray,
                               @ExpectNotNull short[] shortArray,
                               @ExpectNotNull long[] longArray,
                               @ExpectNotNull char[] charArray) {
        objectArray[0] = null;
        byteArray[0] = 0;
        booleanArray[0] = false;
        intArray[0] = 0;
        floatArray[0] = 0;
        doubleArray[0] = 0;
        shortArray[0] = 0;
        longArray[0] = 0;
        charArray[0] = 0;
    }

    public int testUnboxingToPrimitive(@ExpectNotNull Integer i) {
        return i;
    }

    public void testInvokeStaticAssertNotNull(@ExpectNotNull Object o) {
        NullabilityInferenceTestLib.staticAssertNotNull(o);
    }

    public void testInvokeAssertNotNull(@ExpectNotNull Object o) {
        new NullabilityInferenceTestLib().assertNotNull(o);
    }

    public void testInvokeAssertSecondNotNull(Object o1, @ExpectNotNull Object o2) {
        new NullabilityInferenceTestLib().assertSecondNotNull(o1, o2);
    }

    public void testInvokeStaticAssertSecondNotNull(Object o1, @ExpectNotNull Object o2) {
        NullabilityInferenceTestLib.staticAssertSecondNotNull(o1, o2);
    }

    public void testInvokeNullableParameter(Object o) {
        new NullabilityInferenceTestLib().nullableParameter(o);
    }

    @ExpectNotNull
    public Object testInvokeReturnNotNull() {
        return NullabilityInferenceTestLib.returnNotNull();
    }

    @ExpectNullable
    public Object testInvokeReturnNullable() {
        return NullabilityInferenceTestLib.returnNullable();
    }

    @ExpectNotNull
    public Object testReturnAfterInvocation() {
        Object o = getUnannotatedObject();
        o.toString();
        return o;
    }

    @ExpectNotNull
    public Object testReturnAfterPassingAsNotNullArgument() {
        Object o = getUnannotatedObject();
        new NullabilityInferenceTestLib().assertNotNull(o);
        return o;
    }

    @ExpectNotNull
    public Object testConflict(@ExpectNotNull Object o) {
        if (o == null) {
            o.toString();
        }
        return o;
    }

    @ExpectNotNull
    public Object testConflict2(@ExpectNullable Object o) {
        if (o != null) {
            if (o == null) {
                return o;
            }
            return o;
        }
        return "";
    }

    @ExpectNotNull
    public Integer testAutoboxing() {
        return 12;
    }

    @ExpectNullable
    public Object testNullableAfterInstanceOf(@ExpectNullable Object o) {
        if (o instanceof Integer) {
            return o;
        } else if (o instanceof Double) {
            return null;
        }

        return new Object();
    }

    @ExpectNotNull
    public String testNotInstanceOf(@ExpectNullable Object o) {
        if (!(o instanceof String)) {
            return "";
        }
        return (String)o;
    }

    @ExpectNotNull
    public String testNotInstanceOfWithAssignment(@ExpectNullable Object o) {
        if (!(o instanceof String)) {
            o = "";
        }
        return (String)o;
    }

    public String testMultipleInstanceOf(@ExpectNotNull Object o) {
        if (o instanceof Integer) {
            return "N" + ((Integer)o).intValue();
        } else {
            throw new NullPointerException();
        }
    }

    public Object getUnannotatedObject() {
        return null;
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

    @ExpectNotNull
    public <L extends List<String>> L testTrimStringList(@ExpectNotNull L strings)
    {
        for (ListIterator<String> listIt = strings.listIterator(); listIt.hasNext(); )
        {
            String string = listIt.next().trim();
            listIt.set(string);
        }
        return strings;
    }

    @ExpectNotNull
    public int[] testAssertsWithExceptions(@ExpectNotNull int[] a, int[] b) {
        if (a.length != 10) {
            throw new IllegalArgumentException("");
        }
        if (b == null || b.length != 10) {
            b = new int[10];
        } else {
            b[0]++;
        }

        return b;
    }

    public <V> Collection<V> testUnmodifiableCollectionSubclass(Collection<V> collection) {
        if (collection instanceof SortedSet) {
            return Collections.unmodifiableSortedSet((SortedSet<V>) collection);
        } else if (collection instanceof Set) {
            return Collections.unmodifiableSet((Set<V>) collection);
        } else if (collection instanceof List) {
            return Collections.unmodifiableList((List<V>) collection);
        } else {
            return Collections.unmodifiableCollection(collection);
        }
    }

    public static void testArgOfStaticMethod(@ExpectNotNull Object o) {
        o.hashCode();
    }

    public void testArgAssign(@ExpectNotNull String s) {
        s = s.toString();
        System.out.println(s);
    }

    private void error(String s) {
        throw new RuntimeException(s);
    }

    // result nullable is incorrect since there is no path to null result
    @ExpectNotNull
    public String testErrorCall(String s) {
        error(s);
        return null;
    }

    public void methodCallInsideTry(@ExpectNotNull String s) {
        try {
            System.out.print(s.hashCode());
        } catch (IllegalArgumentException e) {
            // Ignore
        }
    }
}
