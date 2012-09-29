package org.jetbrains.kannotator.annotations.io

import java.io.Reader
import javax.xml.parsers.SAXParserFactory
import kotlinlib.buildString
import org.xml.sax.AttributeList
import org.xml.sax.HandlerBase
import java.util.ArrayList

trait AnnotationData {
    val annotationClassFqn: String
    val attributes: Map<String, String>
}

fun parseAnnotations(xml: Reader, handler: (key: String, data: Collection<AnnotationData>) -> Unit, errorHandler: (String) -> Unit) {
    val text = escapeAttributes(xml.readText())
    val parser = SAXParserFactory.newInstance()!!.newSAXParser()!!
    parser.parse(text.getBytes().inputStream, object: HandlerBase(){

        private var currentItemElement: ItemElement? = null

        private class ItemElement(val name: String, val annotations: MutableCollection<AnnotationDataImpl>)
        private class AnnotationDataImpl(override val annotationClassFqn: String, override val attributes: MutableMap<String, String>): AnnotationData

        public override fun startElement(name: String?, attributes: AttributeList?) {
            if (attributes != null) {
                when (name) {
                    "root" -> {}
                    "item" -> {
                        val nameAttrValue = attributes.getValue("name")
                        if (nameAttrValue != null) {
                            currentItemElement = ItemElement(nameAttrValue, ArrayList())
                        }
                        else {
                            errorHandler("NAME attribute for ITEM element is null")
                        }
                    }
                    "annotation" -> {
                        val nameAttrValue = attributes.getValue("name")
                        if (nameAttrValue != null) {
                            currentItemElement!!.annotations.add(AnnotationDataImpl(nameAttrValue, hashMap()))
                        }
                        else {
                            errorHandler("NAME attribute for ANNOTATION element is null")
                        }
                    }
                    "val" -> {
                        val nameAttrValue = attributes.getValue("name")
                        if (nameAttrValue != null) {
                            val valAttrValue = attributes.getValue("val")
                            if (valAttrValue != null) {
                                currentItemElement!!.annotations.last().attributes.put(nameAttrValue, valAttrValue)
                            }
                            else {
                                errorHandler("VAL attribute for VAL element is null")
                            }
                        }
                        else {
                            errorHandler("NAME attribute for VAL element is null")
                        }
                    }
                    else -> {
                        errorHandler("$name tag isn't parsed ")
                    }
                }
            }
            else {
                errorHandler("attributes for $name element are null")
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
    return buildString {
        sb ->
        var inAttribute = false
        for (c in str) {
            when {
                inAttribute && c == '<' -> sb.append("&lt;")
                inAttribute && c == '>' -> sb.append("&gt;")
                c == '\"' || c == '\'' -> {
                    sb.append('\"')
                    inAttribute = !inAttribute
                }
                else -> sb.append(c);
            }
        }
    }
}



