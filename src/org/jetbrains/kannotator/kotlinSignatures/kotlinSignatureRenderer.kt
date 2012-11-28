package org.jetbrains.kannotator.kotlinSignatures

import org.jetbrains.kannotator.declarations.ClassMember
import org.jetbrains.kannotator.annotationsInference.nullability.NullabilityAnnotation
import org.jetbrains.kannotator.annotationsInference.mutability.MutabilityAnnotation
import org.jetbrains.kannotator.declarations.Annotations
import org.jetbrains.kannotator.annotations.io.AnnotationData
import org.jetbrains.kannotator.declarations.Method
import org.jetbrains.kannotator.declarations.Field
import org.jetbrains.kannotator.annotations.io.AnnotationDataImpl
import org.jetbrains.kannotator.declarations.getArgumentTypes
import org.jetbrains.kannotator.declarations.getReturnType
import org.objectweb.asm.Type
import kotlinlib.*
import org.jetbrains.kannotator.annotations.io.getMethodNameAccountingForConstructor
import org.jetbrains.kannotator.asm.util.isPrimitiveOrVoidType
import org.objectweb.asm.Opcodes
import org.jetbrains.kannotator.declarations.isFinal
import org.jetbrains.kannotator.declarations.getType

fun renderKotlinSignature(
        member: ClassMember,
        nullability: Annotations<NullabilityAnnotation>,
        mutability: Annotations<MutabilityAnnotation>
): AnnotationData? {
    val signatureString = when (member) {
        is Method -> renderMethodSignature(member, nullability, mutability)
        is Field -> renderFieldSignature(member, nullability, mutability)
        else -> throw IllegalStateException("Unknown member: $member")
    }
    return AnnotationDataImpl("jet.runtime.typeinfo.KotlinSignature", hashMap("value" to "\"$signatureString\""))
}

fun renderMethodSignature(
        method: Method,
        nullability: Annotations<NullabilityAnnotation>,
        mutability: Annotations<MutabilityAnnotation>
): String {
    // generic parameters (and bounds)
    // return type
    // value parameters
    val genericSignature = method.genericSignature
    if (genericSignature == null) {
        val sb = StringBuilder("fun ${method.getMethodNameAccountingForConstructor()}(")
        // primitives
        // arrays
        // varargs
        val argumentTypes = method.getArgumentTypes()
        for ((i, argType) in argumentTypes.indexed) {
            val last = i == argumentTypes.size - 1
            val vararg = last && method.access.has(Opcodes.ACC_VARARGS)
            if (vararg) {
                sb.append("vararg ")
            }
            sb.append("p$i : ")
            if (vararg) {
                sb.append(renderType(argType.getElementType(), Position.VARARG))
            }
            else {
                sb.append(renderType(argType, Position.IN))
            }
            if (!last) {
                sb.append(", ")
            }
        }
        if (method.name == "<init>") {
            // Constructor
            sb.append(")")
        }
        else {
            sb.append(") : ")
            val returnType = method.getReturnType()
            sb.append(renderType(returnType, Position.OUT))
        }
        return sb.toString()
    }
    else {

        return method.id.methodDesc
    }
}

fun renderFieldSignature(
        field: Field,
        nullability: Annotations<NullabilityAnnotation>,
        mutability: Annotations<MutabilityAnnotation>
): String {
    val sb = StringBuilder()
    sb.append(if (field.isFinal()) "val " else "var ")
    sb.append(field.name)
    sb.append(" : ")
    sb.append(renderType(field.getType(), Position.OUT))
    return sb.toString()
}

enum class Position {
    IN
    OUT
    VARARG
    NONE
}

fun renderType(asmType: Type, position: Position): String {
    return when (asmType.getSort()) {
        Type.VOID -> "Unit"
        Type.BOOLEAN -> "Boolean"
        Type.CHAR -> "Char"
        Type.BYTE -> "Byte"
        Type.SHORT -> "Short"
        Type.INT -> "Int"
        Type.FLOAT -> "Float"
        Type.LONG -> "Long"
        Type.DOUBLE -> "Double"
        Type.ARRAY -> {
            return renderArrayType(asmType.getElementType(), asmType.getDimensions() - 1, position) + "?"
        }
        Type.OBJECT -> mapJavaClass(asmType) + "?"
        else -> throw IllegalArgumentException("Unknown asm type: $asmType")
    }
}

fun renderArrayType(elementType: Type, extraDimensions: Int, position: Position): String {
    return wrapIntoExtraDimensions(extraDimensions, when (elementType.getSort()) {
        Type.BOOLEAN -> "BooleanArray"
        Type.CHAR -> "CharArray"
        Type.BYTE -> "ByteArray"
        Type.SHORT -> "ShortArray"
        Type.INT -> "IntArray"
        Type.FLOAT -> "FloatArray"
        Type.LONG -> "LongArray"
        Type.DOUBLE -> "DoubleArray"
        Type.OBJECT -> "Array<${arrayElementPosition(position)}${renderType(elementType, position)}>"
        else -> throw IllegalArgumentException("impossible array element type: $elementType")
    }, position)
}

fun arrayElementPosition(position: Position): String = if (position != Position.VARARG) "out " else ""

fun wrapIntoExtraDimensions(dimensions: Int, typeString: String, position: Position): String {
    val sb = StringBuilder()
    for (i in 1..dimensions) {
        sb.append("Array<${arrayElementPosition(position)}")
    }
    sb.append(typeString)
    for (i in 1..dimensions) {
        sb.append("?>")
    }
    return sb.toString()
}

fun mapJavaClass(asmType: Type): String {
    return when (asmType.getInternalName()) {
        "java/lang/Object" -> "Any"
        "java/lang/Boolean" -> "Boolean"
        "java/lang/Byte" -> "Byte"
        "java/lang/Short" -> "Short"
        "java/lang/Integer" -> "Int"
        "java/lang/Long" -> "Long"
        "java/lang/Char" -> "Char"
        "java/lang/Float" -> "Float"
        "java/lang/Double" -> "Double"
        "java/lang/String" -> "String"
        "java/lang/Throwable" -> "Throwable"
        else -> asmType.getClassName()!!.suffixAfter(".").replace('$', '.')
    }
}
