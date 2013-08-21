package annotations.io;

/*>>>
import checkers.nullness.quals.*;
import checkers.javari.quals.*;
*/

import annotations.SceneAnnotation;
import annotations.el.*;
import annotations.ext.TypePathEntry;
import annotations.field.*;
import annotations.util.Strings;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.*;

/**
 * IndexFileWriter provides two static methods named <code>write</code>
 * that write a given {@link annotations.el.AScene} to a given {@link java.io.Writer} or filename,
 * in index file format.
 */
public final class IndexFileWriter {
    final /*@ReadOnly*/ AScene scene;

    private static final String INDENT = "    ";

    void printAnnotationDefBody(AnnotationDef d, PrintWriter pw) {
        for (Map. /*@ReadOnly*/ Entry<String, AnnotationFieldType> f : d.fieldTypes.entrySet()) {
            String fieldname = f.getKey();
            AnnotationFieldType fieldType = f.getValue();
            pw.println(INDENT + fieldType + " " + fieldname);
        }
        pw.println();
    }

    private class OurDefCollector extends DefCollector {
        OurDefCollector(PrintWriter pw) throws DefException {
            super(IndexFileWriter.this.scene);
            this.pw = pw;
        }

        final PrintWriter pw;

        @Override
        protected void visitAnnotationDef(AnnotationDef d) {
            pw.println("package " + IOUtils.packagePart(d.name) + ":");
            pw.print("annotation @" + IOUtils.basenamePart(d.name) + ":");
            // TODO: We would only print Retention and Target annotations
            printAnnotations(requiredMetaannotations(d.tlAnnotationsHere),pw);
            pw.println();
            printAnnotationDefBody(d, pw);
        }

        private Collection<SceneAnnotation> requiredMetaannotations(
                Collection<SceneAnnotation> annos) {
            Set<SceneAnnotation> results = new HashSet<SceneAnnotation>();
            for (SceneAnnotation a : annos) {
                String aName = a.def.name;
                if (aName.equals(Retention.class.getCanonicalName())
                        || aName.equals(Target.class.getCanonicalName())) {
                    results.add(a);
                }
            }
            return results;
        }
    }


    private void printValue(AnnotationFieldType aft, Object o, PrintWriter pw) {
        if (aft instanceof AnnotationAFT) {
            printAnnotation((SceneAnnotation) o,pw);
        } else if (aft instanceof ArrayAFT) {
            ArrayAFT aaft = (ArrayAFT) aft;
            pw.print('{');
            if (!(o instanceof List)) {
                printValue(aaft.elementType, o, pw);
            } else {
                /*@ReadOnly*/
                List<?> l =
                        (/*@ReadOnly*/ List<?>) o;
                // watch out--could be an empty array of unknown type
                // (see AnnotationBuilder#addEmptyArrayField)
                if (aaft.elementType == null) {
                    if (l.size() != 0)
                        throw new AssertionError();
                } else {
                    boolean first = true;
                    for (/*@ReadOnly*/ Object o2 : l) {
                        if (!first)
                            pw.print(',');
                        printValue(aaft.elementType, o2, pw);
                        first = false;
                    }
                }
            }
            pw.print('}');
        } else if (aft instanceof ClassTokenAFT) {
            pw.print(aft.format(o));
        } else if (aft instanceof BasicAFT && o instanceof String) {
            pw.print(Strings.escape((String) o));
        } else {
            pw.print(o.toString());
        }
    }

    private void printAnnotation(SceneAnnotation a, PrintWriter pw) {
        pw.print("@" + a.def().name);
        if (!a.fieldValues.isEmpty()) {
            pw.print('(');
            boolean first = true;
            for (Map. /*@ReadOnly*/ Entry<String, /*@ReadOnly*/ Object> f
                    : a.fieldValues.entrySet()) {
                if (!first)
                    pw.print(',');
                pw.print(f.getKey() + "=");
                printValue(a.def().fieldTypes.get(f.getKey()), f.getValue(), pw);
                first = false;
            }
            pw.print(')');
        }
    }

    private void printAnnotations(Collection<? extends SceneAnnotation> annos, PrintWriter pw) {
        for (SceneAnnotation tla : annos) {
            pw.print(' ');
            printAnnotation(tla, pw);
        }
    }

    private void printAnnotations(AElement e, PrintWriter pw) {
        printAnnotations(e.tlAnnotationsHere, pw);
    }

    private void printElement(String indentation,
                              String desc,
                              AElement e,
                              PrintWriter pw) {
        pw.print(indentation + desc + ":");
        printAnnotations(e, pw);
        pw.println();
    }

    /*
    private void printElementAndInnerTypes(String indentation,
            String desc,
            @ReadOnly AElement e) {
        printElement(indentation, desc, e);
        printTypeElementAndInnerTypes(indentation + INDENT, desc, e.type);
    }
    */

    private void printTypeElementAndInnerTypes(String indentation,
                                               String desc,
                                               ATypeElement e,
                                               PrintWriter pw) {
        if (e.tlAnnotationsHere.isEmpty() && e.innerTypes.isEmpty() && desc.equals("type")) {
            return;
        }
        printElement(indentation, desc, e, pw);
        for (Map.Entry<InnerTypeLocation, ATypeElement> ite
                : e.innerTypes.entrySet()) {
            InnerTypeLocation loc = ite.getKey();
            AElement it = ite.getValue();
            pw.print(indentation + INDENT + "inner-type");
            boolean first = true;
            for (TypePathEntry l : loc.location) {
                if (first)
                    pw.print(' ');
                else
                    pw.print(',');
                pw.print(typePathEntryToString(l));
                first = false;
            }
            pw.print(':');
            printAnnotations(it, pw);
            pw.println();
        }
    }

    /**
     * Converts the given {@link annotations.ext.TypePathEntry} to a string of the form
     * {@code tag, arg}, where tag and arg are both integers.
     */
    private String typePathEntryToString(TypePathEntry t) {
        return t.tag.tag + ", " + t.arg;
    }

    private void printNumberedAmbigiousElements(String indentation,
                                                String desc,
                                                Map<Integer, /*@ReadOnly*/ AElement> nels,
                                                PrintWriter pw) {
        for (Map.Entry<Integer, AElement> te
                : nels.entrySet()) {
            AElement t = te.getValue();
            printAmbElementAndInnerTypes(indentation,
                    desc + " #" + te.getKey(), t, pw);
        }
    }

    private void printAmbElementAndInnerTypes(String indentation,
                                              String desc,
                                              AElement e,
                                              PrintWriter pw) {
        printElement(indentation, desc, e, pw);
       /*! if (e.thisType.tlAnnotationsHere.isEmpty() && e.thisType.innerTypes.isEmpty()) {
            return;
        } */
        printElement(indentation + INDENT, "type", e.thisType, pw);
        for (Map. /*@ReadOnly*/ Entry<InnerTypeLocation, /*@ReadOnly*/ ATypeElement> ite
                : e.thisType.innerTypes.entrySet()) {
            InnerTypeLocation loc = ite.getKey();
            /*@ReadOnly*/
            AElement it = ite.getValue();
            pw.print(indentation + INDENT + INDENT + "inner-type");
            boolean first = true;
            for (TypePathEntry l : loc.location) {
                if (first)
                    pw.print(' ');
                else
                    pw.print(',');
                pw.print(typePathEntryToString(l));
                first = false;
            }
            pw.print(':');
            printAnnotations(it, pw);
            pw.println();
        }
    }

    private void printRelativeElements(String indentation,
                                       String desc,
                                       Map<RelativeLocation, ATypeElement> nels,
                                       PrintWriter pw) {
        for (Map.Entry<RelativeLocation, ATypeElement> te
                : nels.entrySet()) {
            ATypeElement t = te.getValue();
            printTypeElementAndInnerTypes(indentation,
                    desc + " " + te.getKey().getLocationString(), t, pw);
        }
    }

    private void printBounds(String indentation, Map<BoundLocation, ATypeElement> bounds, PrintWriter pw) {
        for (Map.Entry<BoundLocation, ATypeElement> be
                : bounds.entrySet()) {
            BoundLocation bl = be.getKey();
            /*@ReadOnly*/
            ATypeElement b = be.getValue();
            if (bl.boundIndex == -1) {
                printTypeElementAndInnerTypes(indentation,
                        "typeparam " + bl.paramIndex, b, pw);
            } else {
                printTypeElementAndInnerTypes(indentation,
                        "bound " + bl.paramIndex + " &" + bl.boundIndex, b, pw);
            }
        }
    }

    private void printExtImpls(String indentation,  Map<TypeIndexLocation,  ATypeElement> extImpls, PrintWriter pw) {

        for (Map.Entry<TypeIndexLocation,  ATypeElement> ei
                : extImpls.entrySet()) {
            TypeIndexLocation idx = ei.getKey();

            ATypeElement ty = ei.getValue();
            // reading from a short into an integer does not preserve sign?
            if (idx.typeIndex == -1 || idx.typeIndex == 65535) {
                printTypeElementAndInnerTypes(indentation, "extends", ty, pw);
            } else {
                printTypeElementAndInnerTypes(indentation, "implements " + idx.typeIndex, ty, pw);
            }
        }
    }

    @NotNull
    /**
     * Classes representations in alphabetical order (sorted by canonical names)
     */
    public Map<String, String> getClassRepresentations() {
        TreeMap<String, String> map = new TreeMap<String, String>();
        for (Map.Entry<String, AClass> entry : scene.classes.entrySet()) {
            StringWriter stringWriter = new StringWriter();
            PrintWriter pw = new PrintWriter(stringWriter);


            writeClass(entry, pw);
            pw.flush();
            map.put(entry.getKey(), stringWriter.toString());
        }
        return map;
    }

    private void writeClass(Map.Entry<String, AClass> ce, PrintWriter pw) {
        String cname = ce.getKey();
        AClass c = ce.getValue();
        String pkg = IOUtils.packagePart(cname);
        String basename = IOUtils.basenamePart(cname);
        pw.println("package " + pkg + ":");
        pw.print("class " + basename + ":");
        printAnnotations(c, pw);
        pw.print("\n");

        printBounds(INDENT, c.bounds, pw);
        printExtImpls(INDENT, c.extendsImplements,pw);

        for (Map.Entry<String, AElement> fe : c.fields.entrySet()) {
            String fname = fe.getKey();
            AElement f = fe.getValue();
            pw.println();
            printElement(INDENT, "field " + fname, f, pw);
            printTypeElementAndInnerTypes(INDENT + INDENT, "type", f.thisType, pw);
        }
        for (Map.Entry<String, AMethod> me
                : c.methods.entrySet()) {
            String mkey = me.getKey();
            AMethod m = me.getValue();
            pw.println();
            printElement(INDENT, "method " + mkey, m, pw);
            printBounds(INDENT + INDENT, m.bounds, pw);
            printTypeElementAndInnerTypes(INDENT + INDENT, "return", m.returnType, pw);
            if (!m.receiver.tlAnnotationsHere.isEmpty() || !m.receiver.innerTypes.isEmpty()) {
                // Only output the receiver if there is something to say. This is a bit
                // inconsistent with the return type, but so be it.
                printTypeElementAndInnerTypes(INDENT + INDENT, "receiver", m.receiver, pw);
            }
            printNumberedAmbigiousElements(INDENT + INDENT, "parameter", m.parameters, pw);
            for (Map.Entry<LocalLocation, AElement> le
                    : m.locals.entrySet()) {
                LocalLocation loc = le.getKey();
                AElement l = le.getValue();
                printElement(INDENT + INDENT,
                        "local " + loc.index + " #"
                                + loc.scopeStart + "+" + loc.scopeLength, l, pw);
                printTypeElementAndInnerTypes(INDENT + INDENT + INDENT,
                        "type", l.thisType, pw);
            }
            printRelativeElements(INDENT + INDENT, "typecast", m.typecasts, pw);
            printRelativeElements(INDENT + INDENT, "instanceof", m.instanceofs, pw);
            printRelativeElements(INDENT + INDENT, "new", m.news, pw);
            // throwsException field is not processed.  Why?
        }
        pw.println();
    }

    private void write(PrintWriter pw) throws DefException {
        // First the annotation definitions...
        new OurDefCollector(pw).visit();

        // And then the annotated classes
        for (Map.Entry<String, AClass> ce : scene.classes.entrySet())
            writeClass(ce, pw);
    }

    public void perform(Writer out) throws DefException{
        PrintWriter pw = new PrintWriter(out);
        write(pw);
        pw.flush();
    }
    public IndexFileWriter(final AScene scene) throws DefException {
        this.scene = scene;
    }

    /**
     * Writes the annotations in <code>scene</code> and their definitions to
     * <code>out</code> in index file format.
     *
     * <p>
     * An {@link annotations.el.AScene} can contain several annotations of the same type but
     * different definitions, while an index file can accommodate only a single
     * definition for each annotation type.  This has two consequences:
     *
     * <ul>
     * <li>Before writing anything, this method uses a {@link annotations.el.DefCollector} to
     * ensure that all definitions of each annotation type are identical
     * (modulo unknown array types).  If not, a {@link annotations.el.DefException} is thrown.
     * <li>There is one case in which, even if a scene is written successfully,
     * reading it back in produces a different scene.  Consider a scene
     * containing two annotations of type Foo, each with an array field bar.
     * In one annotation, bar is empty and of unknown element type (see
     * {@link annotations.AnnotationBuilder#addEmptyArrayField}); in the other, bar is
     * of known element type.  This method will
     * {@linkplain annotations.el.AnnotationDef#unify unify} the two definitions of Foo by
     * writing a single definition with known element type.  When the index
     * file is read into a new scene, the definitions of both annotations
     * will have known element type, whereas in the original scene, one had
     * unknown element type.
     * </ul>
     */
    public static void write(
            /*@ReadOnly*/ AScene scene,
            Writer out) throws DefException {
        new IndexFileWriter(scene).perform(out);
    }

    /**
     * Writes the annotations in <code>scene</code> and their definitions to
     * the file <code>filename</code> in index file format; see
     * {@link #write(annotations.el.AScene, java.io.Writer)}.
     */
    public static void write(
            AScene scene,
            String filename) throws IOException, DefException {
        write(scene, new FileWriter(filename));
    }
}
