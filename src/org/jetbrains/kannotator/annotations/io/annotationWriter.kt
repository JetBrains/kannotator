package org.jetbrains.kannotator.annotations.io

import java.io.Writer
import java.io.File
import java.util.Collections
import kotlinlib.*
import org.jetbrains.kannotator.main.*
import java.util.HashSet
import java.io.FileWriter
import org.jetbrains.kannotator.index.AnnotationKeyIndex
import org.jetbrains.kannotator.index.DeclarationIndex
import java.io.FileReader
import java.util.HashMap
import org.jetbrains.kannotator.annotationsInference.nullability.*
import java.util.LinkedHashMap
import org.jetbrains.kannotator.declarations.*
import org.jetbrains.kannotator.annotationsInference.propagation.*
import org.jetbrains.kannotator.controlFlow.builder.analysis.NullabilityKey
import org.jetbrains.kannotator.ErrorHandler
import annotations.el.AScene
import annotations.io.IndexFileWriter
import annotations.el.AClass
import annotations.AnnotationFactory
import annotations.el.AnnotationDef
import annotations.SceneAnnotation
import annotations.field.AnnotationFieldType
import annotations.field.ClassTokenAFT
import annotations.field.BasicAFT

fun writeAnnotationsToJaif(
        declIndex: DeclarationIndex,
        destRoot: File,
        nullability: Annotations<NullabilityAnnotation>,
        propagatedNullabilityPositions: Set<AnnotationPosition>,
        classPrefixesToOmit: Set<String> = Collections.emptySet(),
        includedClassNames: Set<String> = Collections.emptySet(),
        includedPositions: Set<AnnotationPosition> = Collections.emptySet()
) {
    val scene = buildAnnotationsDataMap(
            declIndex,
            nullability,
            propagatedNullabilityPositions,
            classPrefixesToOmit,
            includedClassNames,
            includedPositions)
            .toAScene()
    val writer = FileWriter(File(destRoot, "annotations.jaif"))
    IndexFileWriter.write(scene, writer)
}


fun writeAnnotationsToXML(writer: Writer, annotations: Map<AnnotationPosition, Collection<AnnotationData>>) {
    val sb = StringBuilder()
    val printer = XmlPrinter(sb)
    printer.openTag("root")
    printer.pushIndent()
    for ((typePosition, annotationDatas) in annotations) {
        printer.openTag("item", hashMap("name" to typePosition.toAnnotationKey()))
        printer.pushIndent()
        for (annotationData in annotationDatas) {
            if (annotationData.attributes.size() < 1) {
                printer.openTag("annotation", hashMap("name" to annotationData.annotationClassFqn), true)
            } else {
                printer.openTag("annotation", hashMap("name" to annotationData.annotationClassFqn))
                for ((name, value) in annotationData.attributes) {
                    val attributesMap = LinkedHashMap<String, String>()
                    attributesMap.put("name", name)
                    attributesMap.put("val", value)
                    printer.pushIndent()
                    printer.openTag("val", attributesMap, true, '"')
                    printer.popIndent()
                }
                printer.closeTag("annotation")
            }
        }
        printer.popIndent()
        printer.closeTag("item")
    }
    printer.popIndent()
    printer.closeTag("root")

    writer.write(sb.toString())
    writer.close()

}

class XmlPrinter(val sb: StringBuilder) {
    private val INDENTATION_UNIT = "    ";
    private var indent = "";

    public fun println() {
        sb.println()
    }

    fun openTag(tagName: String, attributes: Map<String, String>? = null, isClosed: Boolean = false, quoteChar : Char = '\'') {
        sb.append(indent)
        sb.append("<").append(tagName)
        if (attributes != null) {
            for ((name, value) in attributes) {
                sb.append(" ").append(escape(name)).append("=").append(quoteChar).append(escape(value)).append(quoteChar)
            }
        }
        if (isClosed) {
            sb.append("/>")
        }
        else {
            sb.append(">")
        }
        println()
    }

    fun closeTag(tagName: String) {
        sb.append(indent);
        sb.append("</").append(tagName).append(">")
        println()
    }

    public fun pushIndent() {
        indent += INDENTATION_UNIT;
    }

    public fun popIndent() {
        if (indent.length() < INDENTATION_UNIT.length()) {
            throw IllegalStateException("No indentation to pop");
        }

        indent = indent.substring(INDENTATION_UNIT.length());
    }
}

private fun escape(str: String): String {
    return buildString {
        sb ->
        for (c in str) {
            when {
                c == '<' -> sb.append("&lt;")
                c == '>' -> sb.append("&gt;")
                c == '\"' || c == '\'' -> {
                    sb.append("&quot;")
                }
                else -> sb.append(c);
            }
        }
    }
}

fun methodsToAnnotationsMap(
        members: Collection<ClassMember>,
        nullability: Annotations<NullabilityAnnotation>,
        propagatedNullabilityPositions: Set<AnnotationPosition>
): Map<AnnotationPosition, MutableList<AnnotationData>> {
    val annotations = LinkedHashMap<AnnotationPosition, MutableList<AnnotationData>>()

    fun processPosition(pos: AnnotationPosition) {
        val nullAnnotation = nullability[pos]
        if (nullAnnotation == NullabilityAnnotation.NOT_NULL) {
            val data = AnnotationDataImpl(JB_NOT_NULL, hashMap())
            annotations[pos] = arrayListOf<AnnotationData>(data)
            if (pos in propagatedNullabilityPositions) {
                val map = HashMap<String, String>()
                map["value"] = "{${javaClass<NullabilityKey>().getName()}.class}"
                annotations[pos]!!.add(AnnotationDataImpl(JB_PROPAGATED, map))
            }
        }
    }

    for (m in members) {
        if (m is Method) {
            PositionsForMethod(m).forEachValidPosition {pos -> processPosition(pos)}
        } else if (m is Field) {
            processPosition(getFieldTypePosition(m))
        }
    }
    return annotations
}

fun AnnotationPosition.getPackageName(): String? {
    val member = member
    return if (member is Method || member is Field) member.getInternalPackageName() else null
}

fun buildAnnotationsDataMap(
        declIndex: DeclarationIndex,
        nullability: Annotations<NullabilityAnnotation>,
        propagatedNullabilityPositions: Set<AnnotationPosition>,
        classPrefixesToOmit: Set<String>,
        includedClassNames: Set<String>,
        includedPositions: Set<AnnotationPosition>
): Map<AnnotationPosition, MutableList<AnnotationData>> {
    val members = HashSet<ClassMember>()
    nullability.forEach {
        pos, ann ->
        val member = pos.member
        val classDecl = declIndex.findClass(member.declaringClass)
        if ((includedClassNames.contains(member.declaringClass.internal) || (classDecl != null && classDecl.isPublic())) && (includedPositions.contains(pos) || member.isPublicOrProtected())) {
            members.add(member)
        }
    }

    return methodsToAnnotationsMap(
            members.sortByToString().filter { method ->
                !classPrefixesToOmit.any{p -> method.declaringClass.internal.startsWith(p)}
            },
            nullability,
            propagatedNullabilityPositions
    )
}

fun writeAnnotationsToXMLByPackage(
        keyIndex: AnnotationKeyIndex,
        declIndex: DeclarationIndex,
        srcRoot: File?,
        destRoot: File,
        nullability: Annotations<NullabilityAnnotation>,
        propagatedNullabilityPositions: Set<AnnotationPosition>,
        errorHandler: ErrorHandler,
        classPrefixesToOmit: Set<String> = Collections.emptySet(),
        includedClassNames: Set<String> = Collections.emptySet(),
        includedPositions: Set<AnnotationPosition> = Collections.emptySet()
) {
    val annotations = buildAnnotationsDataMap(declIndex, nullability, propagatedNullabilityPositions, classPrefixesToOmit, includedClassNames, includedPositions)
    val annotationsByPackage = HashMap<String, MutableMap<AnnotationPosition, MutableList<AnnotationData>>>()
    for ((pos, data) in annotations) {
        val packageName = pos.getPackageName()
        if (packageName != null) {
            val map = annotationsByPackage.getOrPut(packageName!!, {HashMap()})
            map[pos] = data
        }
    }

    for ((path, pathAnnotations) in annotationsByPackage) {
        val destDir = if (path != "") File(destRoot, path) else destRoot
        destDir.mkdirs()

        if (srcRoot != null) {
            val srcDir = if (path != "") File(srcRoot, path) else srcRoot
            val srcFile = File(srcDir, "annotations.xml")

            if (srcFile.exists()) {
                FileReader(srcFile) use {
                    parseAnnotations(it, {
                        key, annotations ->
                        val position = keyIndex.findPositionByAnnotationKeyString(key)
                        if (position != null) {
                            for (ann in annotations) {
                                if (ann.annotationClassFqn == "jet.runtime.typeinfo.KotlinSignature") {
                                    pathAnnotations.getOrPut(position!!, { arrayList() }).add(AnnotationDataImpl(ann.annotationClassFqn, /*KT-3344*/HashMap<String, String>(ann.attributes)))
                                }
                            }
                        }
                    }, errorHandler)
                }
            }
        }

        val outFile = File(destDir, "annotations.xml")
        val writer = FileWriter(outFile)
        writeAnnotationsToXML(writer, pathAnnotations)
    }
}