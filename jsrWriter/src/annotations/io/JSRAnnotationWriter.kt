package annotations.io

import org.jetbrains.kannotator.annotations.io.AnnotationData
import java.util.ArrayList
import org.jetbrains.kannotator.annotations.io.IndentationPrinter
import java.io.Writer
import java.util.Collections
import java.util.HashSet
import java.util.HashMap
import org.jetbrains.kannotator.declarations.*

fun writeAnnotationsJSRStyle(writer: Writer, annotations: Map<AnnotationPosition, Collection<AnnotationData>>) {
    val printer = JsrFormatAnnotationPrinter()

    for ((typePosition, annotationDatas) in annotations) {
        printer.declarePackage(typePosition.member.packageName.replace("/", ".")) {
            declareClass(typePosition.member.declaringClass.simple) {
                when (typePosition) {
                    is MethodTypePosition ->
                        declareMethod(typePosition.method.id.toString()) {
                            val positionWithinDeclaration = typePosition.relativePosition
                            when (positionWithinDeclaration) {
                                RETURN_TYPE ->
                                    declare(keyword = "return",
                                            annotations = annotationDatas)
                                is ParameterPosition ->
                                    declare(keyword = "parameter ${org.jetbrains.kannotator.annotations.io.correctIfNotStatic(typePosition.method, positionWithinDeclaration.index)}",
                                            annotations = annotationDatas)
                                else -> throw IllegalStateException("Unknown position.")
                            }
                        }
                    is FieldTypePosition ->
                        declareField(typePosition.member.name, annotationDatas)
                    else -> throw IllegalStateException("Unknown type position")
                }
            }
        }
    }
    writer.write(printer.annotationsDeclarationText())
    writer.write(printer.bodyText())
    writer.close()
}

private class JsrFormatAnnotationPrinter : IndentationPrinter() {

    private val sb = StringBuilder()

    private var currentPackage: String? = null
    private var currentClass: String? = null
    private var currentMethod: String? = null
    private var usedAnnotations = HashSet<String>()

    public fun annotationsDeclarationText(): String {
        val packageToAnnotations: MutableMap<String, MutableList<String>> = HashMap()
        for (annotation in usedAnnotations) {
            //TODO: default package
            val packageFQName = annotation.substring(0, annotation.lastIndexOf("."))
            val annotationSimpleName = annotation.substring(annotation.lastIndexOf(".") + 1)
            packageToAnnotations.getOrPut(packageFQName, { ArrayList() }).add(annotationSimpleName)
        }
        val packageDeclarationSB = StringBuilder()
        for ((packageFQName, annotationNamesInPackage) in packageToAnnotations) {
            packageDeclarationSB.append("package $packageFQName:\n")
            for (annotationName in annotationNamesInPackage) {
                packageDeclarationSB.append("annotation @$annotationName:\n")
                //TODO: annotation params
            }
        }
        return packageDeclarationSB.toString()
    }

    public fun declarePackage(packageName: String, block: JsrFormatAnnotationPrinter.() -> Unit) {
        if (currentPackage != packageName) {
            sb.append(indent).append("package $packageName:\n")
            currentPackage = packageName
            currentClass = null
            currentMethod = null
        }
        block()
    }

    public fun declareClass(className: String, block: (JsrFormatAnnotationPrinter.() -> Unit)? = null) {
        if (className != currentClass) {
            declare("class", className, block)
            currentClass = className
            currentMethod = null
        }
        else {
            printBlock(block)
        }
    }

    public fun declareMethod(methodName: String, block: (JsrFormatAnnotationPrinter.() -> Unit)? = null) {
        if (methodName != currentMethod) {
            declare("method", methodName, block)
            currentMethod = methodName
        }
        else {
            printBlock(block)
        }
    }

    public fun declareField(fieldName: String, annotations: Collection<AnnotationData>) {
        declare("field", fieldName, annotations)
    }

    private fun declare(keyword: String, name: String, block: (JsrFormatAnnotationPrinter.() -> Unit)?) {
        declare(keyword, name, Collections.emptySet(), block)
    }

    public fun declare(keyword: String, name: String? = null, annotations: Collection<AnnotationData>, block: (JsrFormatAnnotationPrinter.() -> Unit)? = null) {
        sb.append(indent).append("$keyword${if (name != null) " $name" else ""}:\n")
        pushIndent()
        putAnnotations(annotations)
        popIndent()
        printBlock(block)
    }

    private fun printBlock(block: (JsrFormatAnnotationPrinter.() -> Unit)? = null) {
        if (block != null) {
            pushIndent()
            block()
            popIndent()
        }
    }

    public fun putAnnotations(annotations: Collection<AnnotationData>) {
        if (annotations.empty) {
            return
        }
        sb.append(indent)
        for (annotation in annotations) {
            sb.append("@${annotation.annotationClassFqn} ")
            //TODO: annotations params
            usedAnnotations.add(annotation.annotationClassFqn)
        }
        sb.append("\n")
    }

    public fun bodyText(): String {
        return sb.toString()
    }
}