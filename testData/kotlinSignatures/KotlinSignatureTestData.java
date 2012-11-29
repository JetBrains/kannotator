package kotlinSignatures;

import jet.runtime.typeinfo.KotlinSignature;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kannotator.runtime.annotations.Mutable;
import org.jetbrains.kannotator.runtime.annotations.ReadOnly;

import java.io.Serializable;
import java.util.*;

public abstract class KotlinSignatureTestData {

    public static abstract class NoAnnotations {

        @KotlinSignature("val fi : Int")
        final int fi = 1;
        @KotlinSignature("val fs : String?")
        final String fs = "";

        @KotlinSignature("var b : Boolean")
        boolean b;
        @KotlinSignature("var by : Byte")
        byte by;
        @KotlinSignature("var s : Short")
        short s;
        @KotlinSignature("var i : Int")
        int i;
        @KotlinSignature("var l : Long")
        long l;
        @KotlinSignature("var f : Float")
        float f;
        @KotlinSignature("var d : Double")
        double d;
        @KotlinSignature("var c : Char")
        char c;

        @KotlinSignature("var ba : BooleanArray?")
        boolean[] ba;
        @KotlinSignature("var bya : ByteArray?")
        byte[] bya;
        @KotlinSignature("var sa : ShortArray?")
        short[] sa;
        @KotlinSignature("var ia : IntArray?")
        int[] ia;
        @KotlinSignature("var la : LongArray?")
        long[] la;
        @KotlinSignature("var fa : FloatArray?")
        float[] fa;
        @KotlinSignature("var da : DoubleArray?")
        double[] da;
        @KotlinSignature("var ca : CharArray?")
        char[] ca;

        @KotlinSignature("var baa : Array<out BooleanArray?>?")
        boolean[][] baa;
        @KotlinSignature("var byaa : Array<out ByteArray?>?")
        byte[][] byaa;
        @KotlinSignature("var saa : Array<out ShortArray?>?")
        short[][] saa;
        @KotlinSignature("var iaa : Array<out IntArray?>?")
        int[][] iaa;
        @KotlinSignature("var laa : Array<out LongArray?>?")
        long[][] laa;
        @KotlinSignature("var faa : Array<out FloatArray?>?")
        float[][] faa;
        @KotlinSignature("var daa : Array<out DoubleArray?>?")
        double[][] daa;
        @KotlinSignature("var caa : Array<out CharArray?>?")
        char[][] caa;

        @KotlinSignature("fun NoAnnotations()")
        NoAnnotations() {}

        @KotlinSignature("fun voidNoArgs() : Unit")
        abstract void voidNoArgs();

        @KotlinSignature("fun voidNoAnnotationString(p0 : String?) : Unit")
        abstract void voidNoAnnotationString(String p0);

        @KotlinSignature("fun vararg(vararg p0 : String?) : Unit")
        abstract void vararg(String... p0);

        @KotlinSignature("fun arrayOfArray(p0 : Array<out Array<out Any?>?>?) : Unit")
        void arrayOfArray(Object[][] p0) {}

        interface A {
            interface B {}
        }

        @KotlinSignature("fun innerClass(p0 : KotlinSignatureTestData.NoAnnotations.A.B?) : Unit")
        void innerClass(A.B p0) {}

        @KotlinSignature("fun sb(p0 : StringBuilder?) : Unit")
        void sb(StringBuilder p0) {}
    }

    public static abstract class Nullability {

        @KotlinSignature("var nnia : IntArray")
        @NotNull int[] nnia;
        @KotlinSignature("var nniaa : Array<out IntArray?>")
        @NotNull int[][] nniaa;
        @KotlinSignature("var nnIaa : Array<out Array<out Int?>?>")
        @NotNull Integer[][] nnIaa;

        @KotlinSignature("var sb : StringBuilder")
        @NotNull StringBuilder sb;
        @KotlinSignature("var str : String")
        @NotNull public String str;

        @KotlinSignature("fun nullableInteger() : Int?")
        abstract @Nullable Integer nullableInteger();

        @KotlinSignature("fun notNullInteger() : Int")
        abstract @NotNull Integer notNullInteger();

        @KotlinSignature("fun voidNullableString(p0 : String?) : Unit")
        abstract void voidNullableString(@Nullable String p0);

        @KotlinSignature("fun voidNotNullString(p0 : String) : Unit")
        abstract void voidNotNullString(@NotNull String p0);

        @KotlinSignature("fun staticVoidNullableString(p0 : String?) : Unit")
        static void staticVoidNullableString(@Nullable String p0) {}

        @KotlinSignature("fun staticVoidNotNullString(p0 : String) : Unit")
        static void staticVoidNotNullString(@NotNull String p0) {}

    }

    public static abstract class NoAnnotationsGeneric<C> {
        @KotlinSignature("fun NoAnnotationsGeneric(p0 : Any?)")
        <T> NoAnnotationsGeneric(T p0) {}

        @KotlinSignature("fun NoAnnotationsGeneric(p0 : Class<Any?>?)")
        <T> NoAnnotationsGeneric(Class<T> p0) {}

        @KotlinSignature("fun NoAnnotationsGeneric(p0 : Runnable?)")
        <T extends Runnable> NoAnnotationsGeneric(T p0) {}

        @KotlinSignature("fun NoAnnotationsGeneric(p0 : List<Runnable?>?)")
        <T extends Runnable> NoAnnotationsGeneric(List<T> p0) {}

//        @KotlinSignature("fun NoAnnotationsGeneric(t : {Runnable & Serializable}?)")
//        <T extends Runnable & Serializable> NoAnnotationsGeneric() {}

        @KotlinSignature("fun unboundedWildcard(p0 : List<*>?) : Unit")
        abstract void unboundedWildcard(List<?> p0);

        @KotlinSignature("fun <T> generic(t : T?, ts : List<T>?, tss : Collection<out T>?, tsss : List<in Collection<out T>?>?) : Unit")
        abstract <T> void generic(T t, List<T> ts, Collection<? extends T> tss, List<? super Collection<? extends T>> tsss);
        @KotlinSignature("fun <T : Serializable?> genericOneBound() : Unit")
        abstract <T extends Serializable> void genericOneBound();
        @KotlinSignature("fun <T> genericTwoBounds() : Unit where T : Serializable?, T : Runnable?")
        abstract <T extends Serializable & Runnable> void genericTwoBounds();
        @KotlinSignature("fun <T, R : T> genericTwoParameters() : Unit")
        abstract <T, R extends T> void genericTwoParameters();

        @KotlinSignature("fun <T> typeParametersAreTypeArguments(ts : List<T>?, cs : List<C>?) : Unit")
        <T> void typeParametersAreTypeArguments(List<T> ts, List<C> cs) {}
        @KotlinSignature("fun <T> standaloneTypeParameters(t : T?, c : C?) : Unit")
        <T> void standaloneTypeParameters(T t, C c) {}

        @KotlinSignature("fun <T, R : T, X : C> typeParametersInUpperBounds(ts : List<out T>?, cs : List<out C>?) : Unit")
        <T, R extends T, X extends C> void typeParametersInUpperBounds(List<? extends T> ts, List<? extends C> cs) {}
        @KotlinSignature("fun <T> typeParametersInLowerBounds(ts : List<in T>?, cs : List<in C>?) : Unit")
        <T> void typeParametersInLowerBounds(List<? super T> ts, List<? super C> cs) {}

    }

    public static abstract class Mutability {
        abstract void readOnlyIterator(@ReadOnly Iterator<String> p0);
        abstract void readOnlyIterable(@ReadOnly Iterable<String> p0);
        abstract void readOnlyCollection(@ReadOnly Collection<String> p0);
        abstract void readOnlyList(@ReadOnly List<String> p0);
        abstract void readOnlySet(@ReadOnly Set<String> p0);
        abstract void readOnlyMap(@ReadOnly Map<String, Integer> p0);
        abstract void mutableIterator(@Mutable Iterator<String> p0);

        abstract void mutableIterable(@Mutable Iterable<String> p0);
        abstract void mutableCollection(@Mutable Collection<String> p0);
        abstract void mutableList(@Mutable List<String> p0);
        abstract void mutableSet(@Mutable Set<String> p0);
        abstract void mutableMap(@Mutable Map<String, Integer> p0);

        abstract void readOnlyArrayList(@ReadOnly ArrayList<String> p0);
        abstract void mutableArrayList(@Mutable ArrayList<String> p0);

        abstract void inMutableIterator(@Mutable Iterator<? super String> p0);
        abstract void inMutableIterable(@Mutable Iterable<? super String> p0);
        abstract void inMutableCollection(@Mutable Collection<? super String> p0);
        abstract void inMutableList(@Mutable List<? super String> p0);
        abstract void inMutableSet(@Mutable Set<? super String> p0);
        abstract void inMutableMap(@Mutable Map<? super String, ? super Integer> p0);

        abstract void outMutableIterator(@Mutable Iterator<? extends String> p0);
        abstract void outMutableIterable(@Mutable Iterable<? extends String> p0);
        abstract void outMutableCollection(@Mutable Collection<? extends String> p0);
        abstract void outMutableList(@Mutable List<? extends String> p0);
        abstract void outMutableSet(@Mutable Set<? extends String> p0);
        abstract void outMutableMap(@Mutable Map<? extends String, ? extends Integer> p0);

        abstract void nullableMutableIterator(@Nullable @Mutable Iterator<String> p0);
        abstract void notNullReadonlyIterator(@NotNull @ReadOnly Iterator<String> p0);
    }

}
