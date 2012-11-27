package org.jetbrains.kannotator.annotations.io

import java.io.Writer
import java.util.LinkedHashMap
import kotlinlib.buildString
import kotlinlib.println
import org.jetbrains.kannotator.declarations.AnnotationPosition

fun writeAnnotations(writer: Writer, annotations: Collection<Pair<AnnotationPosition, AnnotationData>>) {
    val sb = StringBuilder()
    val printer = XmlPrinter(sb)
    printer.openTag("root")
    printer.pushIndent()
    for ((typePosition, annotationData) in annotations) {
        printer.openTag("item", hashMap("name" to typePosition.toAnnotationKey()))
        printer.pushIndent()
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






