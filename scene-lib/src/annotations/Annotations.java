package annotations;

import java.lang.annotation.RetentionPolicy;
import java.util.*;

import annotations.el.AnnotationDef;
import annotations.field.*;

/*>>>
import checkers.nullness.quals.*;
import checkers.javari.quals.*;
*/

/**
 * This noninstantiable class provides useful static methods related to
 * annotations, following the convention of {@link java.util.Collections}.
 */
public abstract class Annotations {
    private Annotations() {}

    public static Set<SceneAnnotation> noAnnotations;
    public static Map<String, ? extends AnnotationFieldType> noFieldTypes;
    public static Map<String, ? extends Object> noFieldValues;
    public static Set<SceneAnnotation> typeQualifierMetaAnnotations;

    public static EnumAFT aftRetentionPolicy;
    public static AnnotationDef adRetention;
    public static SceneAnnotation aRetentionClass;
    public static SceneAnnotation aRetentionRuntime;
    public static SceneAnnotation aRetentionSource;
    public static Set<SceneAnnotation> asRetentionClass;
    public static Set<SceneAnnotation> asRetentionRuntime;
    public static Set<SceneAnnotation> asRetentionSource;

    public static AnnotationDef adTarget;
    public static SceneAnnotation aTargetTypeUse;

    public static AnnotationDef adNonNull;
    public static SceneAnnotation aNonNull;

    public static AnnotationDef adTypeQualifier;
    public static SceneAnnotation aTypeQualifier;

    /**
     * Annotations that are meta-annotated with themselves.  Due to a flaw
     * in the the Scene Library, it is unable to read them from classfiles.
     * An expedient workaround is to pre-define them, so they never need be
     * read from a classfile.
     */
    public static Set<AnnotationDef> standardDefs;

    // the field types for an annotation with only one field, named "value".
    static Map<String, ? extends AnnotationFieldType>
                          valueFieldTypeOnly(AnnotationFieldType aft) {
        return Collections.singletonMap("value", aft);
    }

    // the field values for an annotation with only one field, named "value".
    public static Map<String, ? extends Object> valueFieldOnly(Object valueValue) {
        return Collections.singletonMap("value", valueValue);
    }

    // Create an annotation definition with only a value field.
    public static AnnotationDef createValueAnnotationDef(String name, Set<SceneAnnotation> metaAnnotations, AnnotationFieldType aft) {
        return new AnnotationDef(name, metaAnnotations, valueFieldTypeOnly(aft));
    }

    // Create an annotation with only a value field.
    public static SceneAnnotation createValueAnnotation(AnnotationDef ad, Object value) {
        return new SceneAnnotation(ad, valueFieldOnly(value));
    }

    public static SceneAnnotation getRetentionPolicyMetaAnnotation(RetentionPolicy rp) {
        switch (rp) {
        case CLASS: return aRetentionClass;
        case RUNTIME: return aRetentionRuntime;
        case SOURCE: return aRetentionSource;
        default:
            throw new Error("This can't happen");
        }
    }

    public static Set<SceneAnnotation> getRetentionPolicyMetaAnnotationSet(RetentionPolicy rp) {
        switch (rp) {
        case CLASS: return asRetentionClass;
        case RUNTIME: return asRetentionRuntime;
        case SOURCE: return asRetentionSource;
        default:
            throw new Error("This can't happen");
        }
    }

    static {
        noAnnotations = Collections.<SceneAnnotation> emptySet();
        noFieldTypes = Collections.<String, AnnotationFieldType> emptyMap();
        noFieldValues = Collections.<String, Object> emptyMap();

        // This is slightly complicated because Retention's definition is
        // meta-annotated by itself, we have to define the annotation
        // before we can create the annotation on it.
        aftRetentionPolicy = new EnumAFT("java.lang.annotation.RetentionPolicy");
        adRetention = new AnnotationDef("java.lang.annotation.Retention");
        adRetention.setFieldTypes(valueFieldTypeOnly(aftRetentionPolicy));
        aRetentionRuntime = createValueAnnotation(adRetention, "RUNTIME");
        adRetention.tlAnnotationsHere.add(aRetentionRuntime);
        aRetentionClass = createValueAnnotation(adRetention, "CLASS");
        aRetentionSource = createValueAnnotation(adRetention, "SOURCE");
        asRetentionClass = Collections.singleton(aRetentionClass);
        asRetentionRuntime = Collections.singleton(aRetentionRuntime);
        asRetentionSource = Collections.singleton(aRetentionSource);

        adTarget = createValueAnnotationDef("java.lang.annotation.Target",
                                            asRetentionRuntime,
                                            new ArrayAFT(new EnumAFT("java.lang.annotation.ElementType")));
        aTargetTypeUse = createValueAnnotation(adTarget,
                                               // Problem:  ElementType.TYPE_USE is defined only in JDK 7.
                                               // need to decide what the canonical format for these strings is.
                                               // Collections.singletonList("java.lang.annotation.ElementType.TYPE_USE")
                                               // This is the way that naively reading them from classfile gives.
                                               Collections.singletonList("TYPE_USE")
                                               );

        typeQualifierMetaAnnotations = new HashSet<SceneAnnotation>();
        typeQualifierMetaAnnotations.add(aRetentionRuntime);
        typeQualifierMetaAnnotations.add(aTargetTypeUse);

        adNonNull = new AnnotationDef("checkers.nullness.quals.NonNull",
                                      typeQualifierMetaAnnotations,
                                      noFieldTypes);
        aNonNull = new SceneAnnotation(adNonNull, noFieldValues);

        adTypeQualifier = new AnnotationDef("checkers.quals.TypeQualifier",
                                            asRetentionRuntime,
                                            noFieldTypes);
        aTypeQualifier = new SceneAnnotation(adTypeQualifier, noFieldValues);

        standardDefs = new LinkedHashSet<AnnotationDef>();
        standardDefs.add(adTarget);
        standardDefs.add(adRetention);
        // Because annotations can be read from classfiles, it isn't really
        // necessary to add any more here.

    }


    /**
     * Converts the given scalar annotation field value to one appropriate for
     * passing to an {@link AnnotationBuilder} created by <code>af</code>.
     * Conversion is only necessary if <code>x</code> is a subannotation, in
     * which case we rebuild it with <code>af</code> since
     * {@link AnnotationBuilder#addScalarField addScalarField} of an
     * {@link AnnotationBuilder} created by <code>af</code> only accepts
     * subannotations built by <code>af</code>.
     */
    private static /*@ReadOnly*/ Object convertAFV(ScalarAFT aft, /*@ReadOnly*/ Object x) {
        if (aft instanceof AnnotationAFT)
            return rebuild((SceneAnnotation) x);
        else
            return x;
    }

    /**
     * Rebuilds the annotation <code>a</code> using the factory
     * <code>af</code> by iterating through its fields according to its
     * definition and getting the values with {@link SceneAnnotation#getFieldValue}.
     * Returns null if the factory is not interested in <code>a</code>.
     */
    public static final SceneAnnotation rebuild(SceneAnnotation a) {
        AnnotationBuilder ab = AnnotationFactory.saf.beginAnnotation(a.def());
        if (ab != null) {
            for (Map. /*@ReadOnly*/ Entry<String, AnnotationFieldType> fieldDef
                    : a.def().fieldTypes.entrySet()) {

                String fieldName = fieldDef.getKey();
                AnnotationFieldType fieldType =
                        fieldDef.getValue();
                /*@ReadOnly*/ Object fieldValue =
                        a.getFieldValue(fieldName);

                /*@ReadOnly*/ Object nnFieldValue;
                if (fieldValue != null)
                    nnFieldValue = fieldValue;
                else throw new IllegalArgumentException(
                        "annotation has no field value");

                if (fieldType instanceof ArrayAFT) {
                    ArrayAFT aFieldType =
                            (ArrayAFT) fieldType;
                    ArrayBuilder arrb =
                            ab.beginArrayField(fieldName, aFieldType);
                    /*@ReadOnly*/ List<? extends /*@ReadOnly*/ Object> l =
                            (/*@ReadOnly*/ List<? extends /*@ReadOnly*/ Object>) fieldValue;
                    ScalarAFT nnElementType;
                    if (aFieldType.elementType != null)
                        nnElementType = aFieldType.elementType;
                    else
                        throw new IllegalArgumentException(
                                "annotation field type is missing element type");
                    for (/*@ReadOnly*/ Object o : l)
                        arrb.appendElement(convertAFV(
                                nnElementType, o));
                    arrb.finish();
                } else {
                    ScalarAFT sFieldType =
                            (ScalarAFT) fieldType;
                    ab.addScalarField(fieldName, sFieldType,
                            convertAFV(sFieldType, fieldValue));
                }
            }
            return ab.finish();
        } else
            return null;
    }

}
