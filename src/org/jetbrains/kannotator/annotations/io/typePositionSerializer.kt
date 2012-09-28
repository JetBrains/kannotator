package org.jetbrains.kannotator.annotations.io

import kotlinlib.join
import org.jetbrains.kannotator.declarations.ClassName
import org.jetbrains.kannotator.declarations.Method
import org.jetbrains.kannotator.declarations.ParameterPosition
import org.jetbrains.kannotator.declarations.PositionWithinMethod
import org.jetbrains.kannotator.declarations.RETURN_TYPE
import org.jetbrains.kannotator.declarations.TypePosition
import org.jetbrains.kannotator.declarations.getArgumentTypes
import org.jetbrains.kannotator.declarations.getReturnType
import org.jetbrains.kannotator.declarations.isStatic
import org.objectweb.asm.Type
import org.objectweb.asm.signature.SignatureReader
import org.objectweb.asm.util.TraceSignatureVisitor
import org.jetbrains.kannotator.declarations.isVarargs

// ownerFQN returnType? name(paramType{", "}?) position?
// Example:
// org.objectweb.asm.ClassVisitor org.objectweb.asm.FieldVisitor visitField(int, java.lang.String, java.lang.String, java.lang.String, java.lang.Object)
fun TypePosition.toAnnotationKey(): String {
    return method.toAnnotationKeyPrefix() + positionWithinMethod.toAnnotationKeySuffix(method)
}

private fun Method.toAnnotationKeyPrefix(): String {
    return declaringClass.canonicalName + " " +
           getAnnotationKeyReturnTypeString() +
           getMethodNameAccountingForConstructor() + parameterTypesString()
}

private fun Method.getAnnotationKeyReturnTypeString(): String
        = if (id.methodName == "<init>")
            ""
          else if (genericSignature != null) {
                renderReturnType(genericSignature) + " "
          }
          else canonicalName(getReturnType()) + " "

private fun String.internalNameToCanonical(): String = replace('/', '.').toCanonical()

private val ClassName.canonicalName: String
    get() = internal.internalNameToCanonical()

private fun String.toCanonical(): String = replace('$', '.')

private fun canonicalName(_type: Type): String {
    return _type.getClassName()?.toCanonical() ?: "!null"
}

private fun Method.getMethodNameAccountingForConstructor(): String {
    if (id.methodName == "<init>") return declaringClass.simple
    return id.methodName
}

private fun PositionWithinMethod.toAnnotationKeySuffix(method: Method): String {
    return when (this) {
        RETURN_TYPE -> ""
        is ParameterPosition -> " " + correctIfNotStatic(method, this.index)
        else -> throw IllegalArgumentException("Unknown position: $this")
    }
}

private fun correctIfNotStatic(method: Method, parameterIndex: Int): Int {
    // 'this' has index 0
    return if (method.isStatic()) parameterIndex else parameterIndex - 1
}

private fun Method.parameterTypesString(): String {
    val result = if (genericSignature == null) {
        (id.getArgumentTypes() map {it -> canonicalName(it) }).join(", ", "(", ")")
    }
    else {
        renderMethodParameters(genericSignature)
    }
    if (this.isVarargs()) {
        return result.replaceAll("""\[\]\)""", "...)")
    }
    return result
}