package org.jetbrains.kannotator.annotations.io

import java.io.Reader
import javax.xml.parsers.SAXParserFactory
import kotlinlib.buildString
import org.xml.sax.AttributeList
import org.xml.sax.HandlerBase
import java.util.ArrayList
import java.io.File
import org.jetbrains.kannotator.index.AnnotationKeyIndex
import java.util.Collections
import java.io.Writer
import java.util.LinkedHashMap
import kotlinlib.buildString
import kotlinlib.println
import org.jetbrains.kannotator.declarations.AnnotationPosition
import org.jetbrains.kannotator.index.AnnotationKeyIndex
import java.io.File
import org.jetbrains.kannotator.annotationsInference.nullability.NullabilityAnnotation
import org.jetbrains.kannotator.index.DeclarationIndex
import junit.framework.TestCase
import junit.framework.Assert.*
import java.io.File
import java.util.Collections
import java.io.PrintStream
import java.io.FileOutputStream
import kotlinlib.*
import org.jetbrains.kannotator.annotations.io.toAnnotationKey
import org.jetbrains.kannotator.main.ProgressMonitor
import org.jetbrains.kannotator.declarations.Method
import java.io.FileInputStream
import java.util.TreeMap
import org.jetbrains.kannotator.annotationsInference.Annotation
import org.jetbrains.kannotator.annotationsInference.nullability.NullabilityAnnotation
import java.util.ArrayList
import util.assertEqualsOrCreate
import org.jetbrains.kannotator.declarations.AnnotationsImpl
import org.jetbrains.kannotator.declarations.AnnotationPosition
import kotlin.test.assertTrue
import org.jetbrains.kannotator.declarations.Annotations
import org.jetbrains.kannotator.main.*
import util.findJarsInLibFolder
import org.jetbrains.kannotator.index.FileBasedClassSource
import org.junit.Assert
import util.*
import java.util.HashSet
import org.jetbrains.kannotator.annotations.io.writeAnnotations
import java.io.FileWriter
import org.jetbrains.kannotator.declarations.PositionsForMethod
import org.jetbrains.kannotator.annotations.io.AnnotationDataImpl
import org.jetbrains.kannotator.kotlinSignatures.renderMethodSignature
import org.jetbrains.kannotator.kotlinSignatures.kotlinSignatureToAnnotationData
import java.io.StringWriter
import org.jetbrains.kannotator.index.AnnotationKeyIndex
import org.jetbrains.kannotator.declarations.MutableAnnotations
import org.jetbrains.kannotator.index.DeclarationIndex
import kotlinSignatures.KotlinSignatureTestData.MutabilityNoAnnotations
import java.io.BufferedReader
import java.io.FileReader
import org.jetbrains.kannotator.annotations.io.AnnotationData
import org.jetbrains.kannotator.declarations.isPublicOrProtected
import org.jetbrains.kannotator.declarations.isPublic
import org.jetbrains.kannotator.annotations.io.parseAnnotations
import java.util.HashMap
import org.jetbrains.kannotator.declarations.forEachValidPosition
import org.jetbrains.kannotator.annotationsInference.nullability.*
import kotlin.dom.addClass
import java.util.LinkedHashMap
import org.jetbrains.kannotator.classHierarchy.*
import org.jetbrains.kannotator.declarations.ClassMember
import org.jetbrains.kannotator.declarations.getInternalPackageName
import org.jetbrains.kannotator.funDependecy.buildFunctionDependencyGraph
import org.jetbrains.kannotator.funDependecy.getTopologicallySortedStronglyConnectedComponents
import org.jetbrains.kannotator.declarations.*

trait AnnotationData {
    val annotationClassFqn: String
    val attributes: Map<String, String>
}

class AnnotationDataImpl(
        override val annotationClassFqn: String,
        override val attributes: MutableMap<String, String>): AnnotationData


fun parseAnnotations(xml: Reader, handler: (key: String, data: Collection<AnnotationData>) -> Unit, errorHandler: (String) -> Unit) {
    val text = escapeAttributes(xml.readText())
    val parser = SAXParserFactory.newInstance()!!.newSAXParser()!!
    parser.parse(text.getBytes().inputStream, object: HandlerBase(){

        private var currentItemElement: ItemElement? = null

        private inner class ItemElement(val name: String, val annotations: MutableCollection<AnnotationDataImpl>)

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
                                currentItemElement!!.annotations.toList().
                                last().attributes.put(nameAttrValue, valAttrValue)
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

fun loadAnnotationsFromLogs(
        sourceFiles: Collection<File>,
        keyIndex: AnnotationKeyIndex
): Annotations<NullabilityAnnotation> {
    val annotations = AnnotationsImpl<NullabilityAnnotation>()

    for (sourceFile in sourceFiles) {
        BufferedReader(FileReader(sourceFile)) use { br->
            val it = br.lineIterator()

            while (it.hasNext()) {
                val key = it.next()
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