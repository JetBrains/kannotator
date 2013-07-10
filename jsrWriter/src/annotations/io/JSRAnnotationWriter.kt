package annotations.io

import org.jetbrains.kannotator.annotations.io.AnnotationData
import java.util.ArrayList
import org.jetbrains.kannotator.annotations.io.IndentationPrinter
import java.io.Writer
import java.util.Collections
import java.util.HashSet
import org.jetbrains.kannotator.declarations.*
import org.jetbrains.kannotator.annotations.io.getPackageName
import java.util.LinkedHashMap
import org.jetbrains.kannotator.annotations.io.groupAnnotationsByPackage
import org.jetbrains.kannotator.annotations.io.correctIfNotStatic

//guided by http://types.cs.washington.edu/annotation-file-utilities/annotation-file-format.html

fun writeAnnotationsJSRStyle(writer: Writer, annotations: Map<AnnotationPosition, Collection<AnnotationData>>) {
    val printer = JsrFormatAnnotationPrinter()
    printer.writeAnnotations(annotations)
    writer.write(printer.annotationsDeclarationText())
    writer.write(printer.bodyText())
    writer.close()
}

fun writeAnnotationsJSRStyleGroupedByPackage(writer: Writer, annotations: Map<AnnotationPosition, Collection<AnnotationData>>) {
    val printer = JsrFormatAnnotationPrinter()
    val annotationsByPackage = groupAnnotationsByPackage(annotations)
    for (annotationsInPackage in annotationsByPackage.values()) {
        printer.writeAnnotations(annotationsInPackage)
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
        val packageToAnnotations: MutableMap<String, MutableList<String>> = LinkedHashMap()
        for (annotation in usedAnnotations) {
            //NOTE: annotations should not be declared in root package
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
            currentPackage = packageName
            currentClass = null
            currentMethod = null
            sb.append(indent).append("package $packageName:\n")
        }
        block()
    }

    public fun declareClass(className: String, block: (JsrFormatAnnotationPrinter.() -> Unit)? = null) {
        if (className != currentClass) {
            currentClass = className
            currentMethod = null
            declare("class", className, block)
        }
        else {
            printBlock(block)
        }
    }

    public fun declareMethod(methodName: String, block: (JsrFormatAnnotationPrinter.() -> Unit)? = null) {
        if (methodName != currentMethod) {
            currentMethod = methodName
            declare("method", methodName, block)
        }
        else {
            printBlock(block)
        }
    }

    public fun declareMethodReturnType(methodName: String, annotations: Collection<AnnotationData>) {
        assert(currentMethod == methodName)
        popIndent()
        //NOTE: declare both annotation on method declaration itself and on return type
        declare("method", methodName, annotations, true)
        pushIndent()
        declare(keyword = "return",
                annotations = annotations,
                annotationsOnSameLine = true)
    }

    public fun declareField(fieldName: String, annotations: Collection<AnnotationData>) {
        currentMethod = null
        declare("field", fieldName, annotations)
    }

    private fun declare(keyword: String, name: String, block: (JsrFormatAnnotationPrinter.() -> Unit)?) {
        declare(keyword, name, Collections.emptySet(), false, block)
    }

    public fun declare(keyword: String, name: String? = null, annotations: Collection<AnnotationData>,
                       annotationsOnSameLine: Boolean = false, block: (JsrFormatAnnotationPrinter.() -> Unit)? = null) {
        sb.append(indent).append("$keyword${if (name != null) " $name" else ""}:")
        if (!annotationsOnSameLine) {
            sb.append("\n")
        } else {
            sb.append(" ")
        }
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
        for (annotation in annotations) {
            usedAnnotations.add(annotation.annotationClassFqn)
        }
        sb.append(indent)
        //TODO: annotations params
        sb.append(annotations.map {"@${it.annotationClassFqn}"}.makeString(" "))
        sb.append("\n")
    }

    public fun bodyText(): String {
        return sb.toString()
    }

    public fun writeAnnotations(annotations: Map<AnnotationPosition, Collection<AnnotationData>>) {
        for ((typePosition, annotationDatas) in annotations) {
            declarePackage(typePosition.member.packageName.replace("/", ".")) {
                declareClass(typePosition.member.declaringClass.simple) {
                    when (typePosition) {
                        is MethodTypePosition ->
                            {
                                val methodName: String = typePosition.method.id.toString()
                                declareMethod(methodName) {
                                    val positionWithinDeclaration = typePosition.relativePosition
                                    when (positionWithinDeclaration) {
                                        RETURN_TYPE -> {
                                            declareMethodReturnType(methodName, annotationDatas)
                                        }
                                        is ParameterPosition ->
                                            declare(keyword = "parameter ${correctIfNotStatic(typePosition.method, positionWithinDeclaration.index)}",
                                                    annotations = annotationDatas)
                                        else -> throw IllegalStateException("Unknown position.")
                                    }
                                }
                            }
                        is FieldTypePosition ->
                            declareField(typePosition.member.name, annotationDatas)
                        else -> throw IllegalStateException("Unknown type position")
                    }
                }
            }
        }
    }
}