package org.jetbrains.kannotator.annotations.io

import java.io.Reader
import javax.xml.parsers.SAXParserFactory
import org.xml.sax.AttributeList
import org.xml.sax.HandlerBase
import java.io.File
import kotlinlib.*
import java.util.ArrayList
import org.jetbrains.kannotator.index.AnnotationKeyIndex
import java.io.BufferedReader
import java.io.FileReader
import org.jetbrains.kannotator.annotationsInference.nullability.*
import org.jetbrains.kannotator.declarations.*
import java.util.regex.Pattern
import org.jetbrains.kannotator.ErrorHandler
import java.util.HashMap

private val ANNOTATION_KEY_PATTERN = Pattern.compile("""(@\w*\s)?(.*)""")

trait AnnotationData {
    val annotationClassFqn: String
    val attributes: Map<String, String>
}

class AnnotationDataImpl(
        override val annotationClassFqn: String,
        override val attributes: MutableMap<String, String>): AnnotationData


fun parseAnnotations(xml: Reader, handler: (key: String, data: Collection<AnnotationData>) -> Unit, errorHandler: ErrorHandler) {
    val text = escapeAttributes(xml.readText())
    val parser = SAXParserFactory.newInstance()!!.newSAXParser()
    parser.parse(text.toByteArray().inputStream, object: HandlerBase(){

        private var currentItemElement: ItemElement? = null

        private inner class ItemElement(val name: String, val annotations: MutableCollection<AnnotationDataImpl>)

        public override fun startElement(name: String, attributes: AttributeList?) {
            if (attributes != null) {
                when (name) {
                    "root" -> {}
                    "item" -> {
                        val nameAttrValue = attributes.getValue("name")
                        if (nameAttrValue != null) {
                            currentItemElement = ItemElement(nameAttrValue, ArrayList())
                        }
                        else {
                            errorHandler.error("NAME attribute for ITEM element is null")
                        }
                    }
                    "annotation" -> {
                        val nameAttrValue = attributes.getValue("name")
                        if (nameAttrValue != null) {
                            currentItemElement!!.annotations.add(AnnotationDataImpl(nameAttrValue, HashMap()))
                        }
                        else {
                            errorHandler.error("NAME attribute for ANNOTATION element is null")
                        }
                    }
                    "val" -> {
                        val nameAttrValue = attributes.getValue("name")
                        if (nameAttrValue != null) {
                            val valAttrValue = attributes.getValue("val")
                            if (valAttrValue != null) {
                                currentItemElement!!.annotations.toList().
                                last().attributes.put(nameAttrValue, valAttrValue)
                            }
                            else {
                                errorHandler.error("VAL attribute for VAL element is null")
                            }
                        }
                        else {
                            errorHandler.error("NAME attribute for VAL element is null")
                        }
                    }
                    else -> {
                        errorHandler.error("$name tag isn't parsed ")
                    }
                }
            }
            else {
                errorHandler.error("attributes for $name element are null")
            }
        }

        public override fun endElement(name: String?) {
            if (name == "item") {
                handler(currentItemElement!!.name, currentItemElement!!.annotations)
            }
        }
    })
}

private fun escapeAttributes(str: String): String {
    return StringBuilder {
        var inAttribute = false
        for (c in str) {
            when {
                inAttribute && c == '<' -> append("&lt;")
                inAttribute && c == '>' -> append("&gt;")
                c == '\"' || c == '\'' -> {
                    append('\"')
                    inAttribute = !inAttribute
                }
                else -> append(c);
            }
        }
    }.toString()
}

fun loadAnnotationsFromLogs(
        sourceFiles: Collection<File>,
        keyIndex: AnnotationKeyIndex
): Annotations<NullabilityAnnotation> {
    val annotations = AnnotationsImpl<NullabilityAnnotation>()

    for (sourceFile in sourceFiles) {
        BufferedReader(FileReader(sourceFile)) use { br->
            val it = br.lineIterator()

            while (it.hasNext()) {
                val firstLine = it.next()
                val matcher = ANNOTATION_KEY_PATTERN.matcher(firstLine)
                if (!matcher.find()) {
                    throw IllegalStateException("Wrong format of input string: $firstLine")
                }
                val key = matcher.group(2)!!
                val value = it.next()

                val pos = keyIndex.findPositionByAnnotationKeyString(key)
                if (pos == null)
                    continue

                val annotation =
                        if (value == "NULLABLE") NullabilityAnnotation.NULLABLE
                        else if (value == "NOT_NULL") NullabilityAnnotation.NOT_NULL
                        else null

                if (annotation != null) {
                    annotations[pos] = annotation
                }
            }
        }
    }

    return annotations
}