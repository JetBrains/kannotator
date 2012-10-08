package org.jetbrains.kannotator.annotations.io

import java.io.Writer
import org.jetbrains.kannotator.declarations.Annotations
import kotlinlib.println
import java.io.File
import org.jetbrains.kannotator.annotationsInference.nullability.NullabilityAnnotation
import java.util.ArrayList
import org.jetbrains.kannotator.declarations.AnnotationsImpl
import org.jetbrains.kannotator.declarations.TypePosition
import java.util.HashMap
import org.jetbrains.kannotator.declarations.Method
import org.jetbrains.kannotator.declarations.ClassName
import annotations.io.MockTypePosition
import java.util.LinkedHashMap
import kotlinlib.buildString


fun writeAnnotations<A>(writer: Writer, annotations: Collection<Annotations<A>>, renderer: (A) -> AnnotationData) {
    val sb = StringBuilder()
    val printer = XmlPrinter(sb)
    printer.openTag("root")
    printer.pushIndent()
    for (annotation in annotations) {
        annotation.forEach {
            typePosition, annotation ->
            printer.openTag("item", hashMap(Pair("name", typePosition.toAnnotationKey())))
            printer.pushIndent()
            val annotationData = renderer(annotation)
            if (annotationData.attributes.size() < 1) {
                printer.openTag("annotation", hashMap(Pair("name", annotationData.annotationClassFqn)), true)
            } else {
                printer.openTag("annotation", hashMap(Pair("name", annotationData.annotationClassFqn)))
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
            printer.popIndent()
            printer.closeTag("item")

        }
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






