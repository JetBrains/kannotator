package kotlinSignatures;

import jet.runtime.typeinfo.KotlinSignature;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kannotator.runtime.annotations.Mutable;
import org.jetbrains.kannotator.runtime.annotations.ReadOnly;

import java.io.File;
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

        @KotlinSignature("var of : Array<out File?>?")
        File[] of;

        @KotlinSignature("fun NoAnnotations()")
        NoAnnotations() {}

        @KotlinSignature("fun NoAnnotations(named : String?)")
        NoAnnotations(String named) {}

        @KotlinSignature("fun voidNoArgs() : Unit")
        abstract void voidNoArgs();

        @KotlinSignature("fun voidNoAnnotationString(p0 : String?) : Unit")
        abstract void voidNoAnnotationString(String p0);

        @KotlinSignature("fun vararg(vararg p0 : String?) : Unit")
        abstract void vararg(String... p0);

        @KotlinSignature("fun varargInt(vararg ints : Int) : Unit")
        void varargInt(int... ints) {}

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
        <T> void generic(T t, List<T> ts, Collection<? extends T> tss, List<? super Collection<? extends T>> tsss) {}
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

        @KotlinSignature("fun returnParameterOfClass() : C?")
        C returnParameterOfClass() {return null; }
        @KotlinSignature("fun returnListOfParameterOfClass() : MutableList<C>?")
        List<C> returnListOfParameterOfClass() {return null; }

        @KotlinSignature("fun <T, R : T, X : C> typeParametersInUpperBounds(ts : List<out T>?, cs : List<out C>?) : Unit")
        <T, R extends T, X extends C> void typeParametersInUpperBounds(List<? extends T> ts, List<? extends C> cs) {}
        @KotlinSignature("fun <T> typeParametersInLowerBounds(ts : List<in T>?, cs : List<in C>?) : Unit")
        <T> void typeParametersInLowerBounds(List<? super T> ts, List<? super C> cs) {}

        static class TwoParams<T, R> {}

        @KotlinSignature("fun twoGenericParams(p0 : KotlinSignatureTestData.NoAnnotationsGeneric.TwoParams<String?, Int?>?) : KotlinSignatureTestData.NoAnnotationsGeneric.TwoParams<String?, Int?>?")
        abstract TwoParams<String, Integer> twoGenericParams(TwoParams<String, Integer> p0);
    }

    public static abstract class WithGenericInner<C> {
        class Inner<T> {}

        @KotlinSignature("fun inner(p0 : KotlinSignatureTestData.WithGenericInner<C>.Inner<String?>?) : KotlinSignatureTestData.WithGenericInner<C>.Inner<String?>?")
        abstract Inner<String> inner(Inner<String> p0);

    }

    public static abstract class MutabilityNoAnnotations {
        @KotlinSignature("fun paramIterator(p0 : Iterator<String?>?) : Unit")
        abstract void paramIterator(Iterator<String> p0);
        @KotlinSignature("fun paramIterator() : MutableIterator<String?>?")
        abstract Iterator<String> paramIterator();

        @KotlinSignature("fun paramIterable(p0 : Iterable<String?>?) : Unit")
        abstract void paramIterable(Iterable<String> p0);
        @KotlinSignature("fun paramIterable() : MutableIterable<String?>?")
        abstract Iterable<String> paramIterable();

        @KotlinSignature("fun paramCollection(p0 : Collection<String?>?) : Unit")
        abstract void paramCollection(Collection<String> p0);
        @KotlinSignature("fun paramCollection() : MutableCollection<String?>?")
        abstract Collection<String> paramCollection();

        @KotlinSignature("fun paramList(p0 : List<String?>?) : Unit")
        abstract void paramList(List<String> p0);
        @KotlinSignature("fun paramList() : MutableList<String?>?")
        abstract List<String> paramList();

        @KotlinSignature("fun paramSet(p0 : Set<String?>?) : Unit")
        abstract void paramSet(Set<String> p0);
        @KotlinSignature("fun paramSet() : MutableSet<String?>?")
        abstract Set<String> paramSet();

        @KotlinSignature("fun paramMap(p0 : Map<String?, Int?>?) : Unit")
        abstract void paramMap(Map<String, Integer> p0);
        @KotlinSignature("fun paramMap() : MutableMap<String?, Int?>?")
        abstract Map<String, Integer> paramMap();

        @KotlinSignature("fun paramMapEntry(p0 : Map.Entry<String?, Int?>?) : Unit")
        abstract void paramMapEntry(Map.Entry<String, Integer> p0);
        @KotlinSignature("fun paramMapEntry() : MutableMap.MutableEntry<String?, Int?>?")
        abstract Map.Entry<String, Integer> paramMapEntry();


        @KotlinSignature("fun inIterator(p0 : Iterator<in String?>?) : Unit")
        abstract void inIterator(Iterator<? super String> p0);
        @KotlinSignature("fun inIterator() : MutableIterator<in String?>?")
        abstract Iterator<? super String> inIterator();

        @KotlinSignature("fun inIterable(p0 : Iterable<in String?>?) : Unit")
        abstract void inIterable(Iterable<? super String> p0);
        @KotlinSignature("fun inIterable() : MutableIterable<in String?>?")
        abstract Iterable<? super String> inIterable();

        @KotlinSignature("fun inCollection(p0 : Collection<in String?>?) : Unit")
        abstract void inCollection(Collection<? super String> p0);
        @KotlinSignature("fun inCollection() : MutableCollection<in String?>?")
        abstract Collection<? super String> inCollection();

        @KotlinSignature("fun inList(p0 : List<in String?>?) : Unit")
        abstract void inList(List<? super String> p0);
        @KotlinSignature("fun inList() : MutableList<in String?>?")
        abstract List<? super String> inList();

        @KotlinSignature("fun inSet(p0 : Set<in String?>?) : Unit")
        abstract void inSet(Set<? super String> p0);
        @KotlinSignature("fun inSet() : MutableSet<in String?>?")
        abstract Set<? super String> inSet();

        @KotlinSignature("fun inMap(p0 : Map<in String?, in Int?>?) : Unit")
        abstract void inMap(Map<? super String, ? super Integer> p0);
        @KotlinSignature("fun inMap() : MutableMap<in String?, in Int?>?")
        abstract Map<? super String, ? super Integer> inMap();

        @KotlinSignature("fun inMapEntry(p0 : Map.Entry<in String?, in Int?>?) : Unit")
        abstract void inMapEntry(Map.Entry<? super String, ? super Integer> p0);
        @KotlinSignature("fun inMapEntry() : MutableMap.MutableEntry<in String?, in Int?>?")
        abstract Map.Entry<? super String, ? super Integer> inMapEntry();


        @KotlinSignature("fun outIterator(p0 : Iterator<out String?>?) : Unit")
        abstract void outIterator(Iterator<? extends String> p0);
        @KotlinSignature("fun outIterator() : MutableIterator<out String?>?")
        abstract Iterator<? extends String> outIterator();

        @KotlinSignature("fun outIterable(p0 : Iterable<out String?>?) : Unit")
        abstract void outIterable(Iterable<? extends String> p0);
        @KotlinSignature("fun outIterable() : MutableIterable<out String?>?")
        abstract Iterable<? extends String> outIterable();

        @KotlinSignature("fun outCollection(p0 : Collection<out String?>?) : Unit")
        abstract void outCollection(Collection<? extends String> p0);
        @KotlinSignature("fun outCollection() : MutableCollection<out String?>?")
        abstract Collection<? extends String> outCollection();

        @KotlinSignature("fun outList(p0 : List<out String?>?) : Unit")
        abstract void outList(List<? extends String> p0);
        @KotlinSignature("fun outList() : MutableList<out String?>?")
        abstract List<? extends String> outList();

        @KotlinSignature("fun outSet(p0 : Set<out String?>?) : Unit")
        abstract void outSet(Set<? extends String> p0);
        @KotlinSignature("fun outSet() : MutableSet<out String?>?")
        abstract Set<? extends String> outSet();

        @KotlinSignature("fun outMap(p0 : Map<out String?, out Int?>?) : Unit")
        abstract void outMap(Map<? extends String, ? extends Integer> p0);
        @KotlinSignature("fun outMap() : MutableMap<out String?, out Int?>?")
        abstract Map<? extends String, ? extends Integer> outMap();

        @KotlinSignature("fun outMapEntry(p0 : Map.Entry<out String?, out Int?>?) : Unit")
        abstract void outMapEntry(Map.Entry<? extends String, ? extends Integer> p0);
        @KotlinSignature("fun outMapEntry() : MutableMap.MutableEntry<out String?, out Int?>?")
        abstract Map.Entry<? extends String, ? extends Integer> outMapEntry();

    }

    public static abstract class Mutability {
        @KotlinSignature("fun readOnlyIterator(p0 : Iterator<String?>?) : Unit")
        abstract void readOnlyIterator(@ReadOnly Iterator<String> p0);
        @KotlinSignature("fun readOnlyIterator() : Iterator<String?>?")
        abstract @ReadOnly Iterator<String> readOnlyIterator();

        @KotlinSignature("fun readOnlyIterable(p0 : Iterable<String?>?) : Unit")
        abstract void readOnlyIterable(@ReadOnly Iterable<String> p0);
        @KotlinSignature("fun readOnlyIterable() : Iterable<String?>?")
        abstract @ReadOnly Iterable<String> readOnlyIterable();

        @KotlinSignature("fun readOnlyCollection(p0 : Collection<String?>?) : Unit")
        abstract void readOnlyCollection(@ReadOnly Collection<String> p0);
        @KotlinSignature("fun readOnlyCollection() : Collection<String?>?")
        abstract @ReadOnly Collection<String> readOnlyCollection();

        @KotlinSignature("fun readOnlyList(p0 : List<String?>?) : Unit")
        abstract void readOnlyList(@ReadOnly List<String> p0);
        @KotlinSignature("fun readOnlyList() : List<String?>?")
        abstract @ReadOnly List<String> readOnlyList();

        @KotlinSignature("fun readOnlySet(p0 : Set<String?>?) : Unit")
        abstract void readOnlySet(@ReadOnly Set<String> p0);
        @KotlinSignature("fun readOnlySet() : Set<String?>?")
        abstract @ReadOnly Set<String> readOnlySet();

        @KotlinSignature("fun readOnlyMap(p0 : Map<String?, Int?>?) : Unit")
        abstract void readOnlyMap(@ReadOnly Map<String, Integer> p0);
        @KotlinSignature("fun readOnlyMap() : Map<String?, Int?>?")
        abstract @ReadOnly Map<String, Integer> readOnlyMap();

        @KotlinSignature("fun readOnlyMapEntry(p0 : Map.Entry<String?, Int?>?) : Unit")
        abstract void readOnlyMapEntry(@ReadOnly Map.Entry<String, Integer> p0);
        @KotlinSignature("fun readOnlyMapEntry() : Map.Entry<String?, Int?>?")
        abstract @ReadOnly Map.Entry<String, Integer> readOnlyMapEntry();


        @KotlinSignature("fun mutableIterator(p0 : MutableIterator<String?>?) : Unit")
        abstract void mutableIterator(@Mutable Iterator<String> p0);
        @KotlinSignature("fun mutableIterator() : MutableIterator<String?>?")
        abstract @Mutable Iterator<String> mutableIterator();


        @KotlinSignature("fun mutableIterable(p0 : MutableIterable<String?>?) : Unit")
        abstract void mutableIterable(@Mutable Iterable<String> p0);
        @KotlinSignature("fun mutableIterable() : MutableIterable<String?>?")
        abstract @Mutable Iterable<String> mutableIterable();

        @KotlinSignature("fun mutableCollection(p0 : MutableCollection<String?>?) : Unit")
        abstract void mutableCollection(@Mutable Collection<String> p0);
        @KotlinSignature("fun mutableCollection() : MutableCollection<String?>?")
        abstract @Mutable Collection<String> mutableCollection();

        @KotlinSignature("fun mutableList(p0 : MutableList<String?>?) : Unit")
        abstract void mutableList(@Mutable List<String> p0);
        @KotlinSignature("fun mutableList() : MutableList<String?>?")
        abstract @Mutable List<String> mutableList();

        @KotlinSignature("fun mutableSet(p0 : MutableSet<String?>?) : Unit")
        abstract void mutableSet(@Mutable Set<String> p0);
        @KotlinSignature("fun mutableSet() : MutableSet<String?>?")
        abstract @Mutable Set<String> mutableSet();

        @KotlinSignature("fun mutableMapEntry(p0 : MutableMap.MutableEntry<String?, Int?>?) : Unit")
        abstract void mutableMapEntry(@Mutable Map.Entry<String, Integer> p0);
        @KotlinSignature("fun mutableMapEntry() : MutableMap.MutableEntry<String?, Int?>?")
        abstract @Mutable Map.Entry<String, Integer> mutableMapEntry();


        @KotlinSignature("fun readOnlyArrayList(p0 : ArrayList<String?>?) : Unit")
        abstract void readOnlyArrayList(@ReadOnly ArrayList<String> p0);
        @KotlinSignature("fun readOnlyArrayList() : ArrayList<String?>?")
        abstract @ReadOnly ArrayList<String> readOnlyArrayList();

        @KotlinSignature("fun mutableArrayList(p0 : ArrayList<String?>?) : Unit")
        abstract void mutableArrayList(@Mutable ArrayList<String> p0);
        @KotlinSignature("fun mutableArrayList() : ArrayList<String?>?")
        abstract @Mutable ArrayList<String> mutableArrayList();


        @KotlinSignature("fun inMutableIterator(p0 : MutableIterator<in String?>?) : Unit")
        abstract void inMutableIterator(@Mutable Iterator<? super String> p0);
        @KotlinSignature("fun inMutableIterator() : MutableIterator<in String?>?")
        abstract @Mutable Iterator<? super String> inMutableIterator();

        @KotlinSignature("fun inMutableIterable(p0 : MutableIterable<in String?>?) : Unit")
        abstract void inMutableIterable(@Mutable Iterable<? super String> p0);
        @KotlinSignature("fun inMutableIterable() : MutableIterable<in String?>?")
        abstract @Mutable Iterable<? super String> inMutableIterable();

        @KotlinSignature("fun inMutableCollection(p0 : MutableCollection<in String?>?) : Unit")
        abstract void inMutableCollection(@Mutable Collection<? super String> p0);
        @KotlinSignature("fun inMutableCollection() : MutableCollection<in String?>?")
        abstract @Mutable Collection<? super String> inMutableCollection();

        @KotlinSignature("fun inMutableList(p0 : MutableList<in String?>?) : Unit")
        abstract void inMutableList(@Mutable List<? super String> p0);
        @KotlinSignature("fun inMutableList() : MutableList<in String?>?")
        abstract @Mutable List<? super String> inMutableList();

        @KotlinSignature("fun inMutableSet(p0 : MutableSet<in String?>?) : Unit")
        abstract void inMutableSet(@Mutable Set<? super String> p0);
        @KotlinSignature("fun inMutableSet() : MutableSet<in String?>?")
        abstract @Mutable Set<? super String> inMutableSet();

        @KotlinSignature("fun inMutableMapEntry(p0 : MutableMap.MutableEntry<in String?, in Int?>?) : Unit")
        abstract void inMutableMapEntry(@Mutable Map.Entry<? super String, ? super Integer> p0);
        @KotlinSignature("fun inMutableMapEntry() : MutableMap.MutableEntry<in String?, in Int?>?")
        abstract @Mutable Map.Entry<? super String, ? super Integer> inMutableMapEntry();


        @KotlinSignature("fun outMutableIterator(p0 : MutableIterator<out String?>?) : Unit")
        abstract void outMutableIterator(@Mutable Iterator<? extends String> p0);
        @KotlinSignature("fun outMutableIterator() : MutableIterator<out String?>?")
        abstract @Mutable Iterator<? extends String> outMutableIterator();

        @KotlinSignature("fun outMutableIterable(p0 : MutableIterable<out String?>?) : Unit")
        abstract void outMutableIterable(@Mutable Iterable<? extends String> p0);
        @KotlinSignature("fun outMutableIterable() : MutableIterable<out String?>?")
        abstract @Mutable Iterable<? extends String> outMutableIterable();

        @KotlinSignature("fun outMutableCollection(p0 : MutableCollection<out String?>?) : Unit")
        abstract void outMutableCollection(@Mutable Collection<? extends String> p0);
        @KotlinSignature("fun outMutableCollection() : MutableCollection<out String?>?")
        abstract @Mutable Collection<? extends String> outMutableCollection();

        @KotlinSignature("fun outMutableList(p0 : MutableList<out String?>?) : Unit")
        abstract void outMutableList(@Mutable List<? extends String> p0);
        @KotlinSignature("fun outMutableList() : MutableList<out String?>?")
        abstract @Mutable List<? extends String> outMutableList();

        @KotlinSignature("fun outMutableSet(p0 : MutableSet<out String?>?) : Unit")
        abstract void outMutableSet(@Mutable Set<? extends String> p0);
        @KotlinSignature("fun outMutableSet() : MutableSet<out String?>?")
        abstract @Mutable Set<? extends String> outMutableSet();

        @KotlinSignature("fun outMutableMapEntry(p0 : MutableMap.MutableEntry<out String?, out Int?>?) : Unit")
        abstract void outMutableMapEntry(@Mutable Map.Entry<? extends String, ? extends Integer> p0);
        @KotlinSignature("fun outMutableMapEntry() : MutableMap.MutableEntry<out String?, out Int?>?")
        abstract @Mutable Map.Entry<? extends String, ? extends Integer> outMutableMapEntry();


        @KotlinSignature("fun nullableMutableIterator(p0 : MutableIterator<String?>?) : Unit")
        abstract void nullableMutableIterator(@Nullable @Mutable Iterator<String> p0);
        @KotlinSignature("fun nullableMutableIterator() : MutableIterator<String?>?")
        abstract @Nullable @Mutable Iterator<String> nullableMutableIterator();

        @KotlinSignature("fun notNullReadonlyIterator(p0 : Iterator<String?>) : Unit")
        abstract void notNullReadonlyIterator(@NotNull @ReadOnly Iterator<String> p0);
        @KotlinSignature("fun notNullReadonlyIterator() : Iterator<String?>")
        abstract @NotNull @ReadOnly Iterator<String> notNullReadonlyIterator();

    }

    public class Inner {
        @KotlinSignature("fun Inner()")
        Inner() {}

        @KotlinSignature("fun Inner(s : String?)")
        Inner(String s) {}
    }

    public static class NamedParametersLongTypes {
        @KotlinSignature("fun longTypes(i : Int, l : Long, o : Any?, d : Double, f : Float?) : Unit")
        void longTypes(int i, long l, Object o, double d, Float f) {}

        @KotlinSignature("fun staticLongTypes(i : Int, l : Long, o : Any?, d : Double, f : Float?) : Unit")
        static void staticLongTypes(int i, long l, Object o, double d, Float f) {}
    }

    public enum Enum {
        A("");

        @KotlinSignature("fun Enum(s : String?)")
        Enum(String s) {}
    }
}
