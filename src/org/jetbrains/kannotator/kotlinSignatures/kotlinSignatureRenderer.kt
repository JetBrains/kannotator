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
import org.jetbrains.kannotator.declarations.PositionsForMethod
import org.jetbrains.kannotator.declarations.getFieldTypePosition
import org.jetbrains.kannotator.declarations.isStatic
import org.objectweb.asm.signature.SignatureVisitor
import org.objectweb.asm.signature.SignatureReader
import java.util.ArrayList
import org.jetbrains.kannotator.declarations.MethodWithNamedParameters

fun renderKotlinSignature(kotlinSignatureString: String): AnnotationData? {
    return AnnotationDataImpl("jet.runtime.typeinfo.KotlinSignature", hashMap("value" to "\"$kotlinSignatureString\""))
}

fun renderMethodSignature(
        methodWithNamedParameters: MethodWithNamedParameters,
        nullability: Annotations<NullabilityAnnotation>,
        mutability: Annotations<MutabilityAnnotation>
): String {
    val method = methodWithNamedParameters.method
    val signature = parseGenericMethodSignature(method.genericSignature ?: method.id.methodDesc)
    val isConstructor = method.name == "<init>"

    val typeParametersByName = signature.typeParameters.toMap {
        param -> param.name to param
    }

    fun substituteIfNeeded(genericType: GenericType): GenericType {
        if (isConstructor) {
            return substitute(genericType) {
                name -> typeParametersByName[name]?.upperBounds?.get(0)
            }
        }
        else {
            return genericType
        }
    }

    val sb = StringBuilder()
    val whereClause = ArrayList<String>()
    sb.append("fun ")

    if (!signature.typeParameters.isEmpty() && !isConstructor) {
        sb.append("<")
        for ((i, param) in signature.typeParameters.indexed) {
            if (i != 0) {
                sb.append(", ")
            }
            sb.append(param.name)

            if (param.hasNontrivialBounds()) {
                fun renderUpperBound(bound: GenericType): String {
                    return renderType(bound, Position.UPPER_BOUND, NullabilityAnnotation.NULLABLE)
                }
                if (param.upperBounds.size == 1) {
                    sb.append(" : ").append(renderUpperBound(param.upperBounds[0]))
                }
                else {
                    for (bound in param.upperBounds) {
                        whereClause.add(param.name + " : " + renderUpperBound(bound))
                    }
                }
            }
        }
        sb.append("> ")
    }

    sb.append(method.getMethodNameAccountingForConstructor())
    sb.append("(")

    for (param in signature.valueParameters) {
        sb.appendParameter(methodWithNamedParameters, param.index) {
            vararg ->
            val nullabilityAnnotation = nullability.getAnnotationForParameter(method, param.index, NullabilityAnnotation.NULLABLE)
            if (vararg) {
                sb.append(renderType(substituteIfNeeded(param.genericType.arrayElementType), Position.VARARG, nullabilityAnnotation))
            }
            else {
                sb.append(renderType(substituteIfNeeded(param.genericType), Position.METHOD_PARAMETER, nullabilityAnnotation))
            }
        }
    }

    if (isConstructor) {
        // Constructor
        sb.append(")")
    }
    else {
        sb.append(") : ")
        val returnType = signature.returnType
        val nullabilityAnnotation = nullability.getAnnotationForReturnType(method, NullabilityAnnotation.NULLABLE)
        sb.append(renderType(returnType, Position.RETURN_TYPE, nullabilityAnnotation))
    }

    if (!whereClause.isEmpty()) {
        sb.append(" where ").append(whereClause.join(", "))
    }

    return sb.toString()
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
    val nullabilityAnnotation = nullability[getFieldTypePosition(field)] ?: NullabilityAnnotation.NULLABLE
    sb.append(renderType(field.getType().toGenericType(), Position.RETURN_TYPE, nullabilityAnnotation))
    return sb.toString()
}

fun Type.toGenericType(): GenericType {
    val signature = "(${this.getDescriptor()})V"
    return parseGenericMethodSignature(signature).valueParameters[0].genericType
}

fun <A> Annotations<A>.getAnnotationForParameter(method: Method, parameterIndex: Int, default: A): A {
    val thisOffset = if (method.isStatic()) 0 else 1
    val positions = PositionsForMethod(method)
    val annotationPosition = positions.forParameter(parameterIndex + thisOffset).position
    return this[annotationPosition] ?: default
}

fun <A> Annotations<A>.getAnnotationForReturnType(method: Method, default: A): A {
    val positions = PositionsForMethod(method)
    val annotationPosition = positions.forReturnType().position
    return this[annotationPosition] ?: default
}

fun StringBuilder.appendParameter(method: MethodWithNamedParameters, parameterIndex: Int, forType: (isVararg: Boolean) -> Unit) {
    if (parameterIndex != 0) {
        append(", ")
    }

    val last = parameterIndex == method.parameterNames.size - 1
    val vararg = last && method.method.access.has(Opcodes.ACC_VARARGS)
    if (vararg) {
        append("vararg ")
    }
    append(method.parameterNames[parameterIndex])
    append(" : ")
    forType(vararg)
}

enum class Position {
    METHOD_PARAMETER
    RETURN_TYPE
    VARARG
    CLASS_TYPE_ARGUMENT
    UPPER_BOUND
}

fun renderType(genericType: GenericType, position: Position, nullability: NullabilityAnnotation): String {
    val classifier = genericType.classifier
    return when (classifier) {
        is BaseType -> renderBaseType(classifier)
        is NamedClass -> renderNamedClass(classifier) + renderArguments(genericType, position) + nullability.suffix()
        is TypeVariable -> renderTypeVariable(classifier, position, nullability)
        Array -> renderArrayType(genericType, position) + nullability.suffix()
        else -> throw IllegalArgumentException("Unknown classifier: $classifier")
    }
}

fun renderArguments(genericType: GenericType, position: Position): String {
    if (genericType.arguments.isEmpty()) return ""
    return buildString {
        sb ->
        sb.append("<")
        for ((i, arg) in genericType.arguments.indexed) {
            if (i > 0) {
                sb.append(", ")
            }
            sb.append(when (arg) {
                UnBoundedWildcard -> "*"
                is BoundedWildcard -> arg.wildcard.projection() + renderType(arg.bound, Position.UPPER_BOUND, NullabilityAnnotation.NULLABLE)
                is NoWildcard -> renderType(arg.genericType, Position.CLASS_TYPE_ARGUMENT, NullabilityAnnotation.NULLABLE)
                else -> throw IllegalStateException(arg.toString())
            })
        }
        sb.append(">")
    }
}

fun Wildcard.projection(): String = when(this) {
    Wildcard.EXTENDS -> "out "
    Wildcard.SUPER -> "in "
}

fun renderBaseType(classifier: BaseType): String {
    return when (classifier.descriptor) {
        'V' -> "Unit"
        'B' -> "Byte"
        'J' -> "Long"
        'Z' -> "Boolean"
        'I' -> "Int"
        'S' -> "Short"
        'C' -> "Char"
        'F' -> "Float"
        'D' -> "Double"
        else -> throw IllegalArgumentException("Unknown base type: ${classifier.descriptor}")
    }
}

fun renderBaseArrayType(classifier: BaseType): String {
    return when (classifier.descriptor) {
        'B' -> "ByteArray"
        'J' -> "LongArray"
        'Z' -> "BooleanArray"
        'I' -> "IntArray"
        'S' -> "ShortArray"
        'C' -> "CharArray"
        'F' -> "FloatArray"
        'D' -> "DoubleArray"
        else -> throw IllegalArgumentException("Unknown base array element type: ${classifier.descriptor}")
    }
}

fun renderTypeVariable(variable: TypeVariable, position: Position, nullability: NullabilityAnnotation): String {
    val nullableByDefault = position !in hashSet(Position.CLASS_TYPE_ARGUMENT, Position.UPPER_BOUND)
    return variable.name + if (nullableByDefault) nullability.suffix() else ""
}

fun renderArrayType(arrayType: GenericType, position: Position): String {
    val elementType = arrayType.arrayElementType
    if (elementType.classifier is BaseType) {
        return renderBaseArrayType(elementType.classifier as BaseType)

    }
    return "Array<${arrayElementPosition(position)}${renderType(elementType, Position.CLASS_TYPE_ARGUMENT, NullabilityAnnotation.NULLABLE)}>"
}

fun arrayElementPosition(position: Position): String = if (position != Position.VARARG) "out " else ""

fun renderNamedClass(namedClass: NamedClass): String {
    return when (namedClass) {
        is ToplevelClass -> when (namedClass.internalName) {
            "java/lang/Object" -> "Any"
            "java/lang/Integer" -> "Int"
            else -> namedClass.internalName.suffixAfter("/").replace('$', '.')
        }
        is InnerClass -> renderNamedClass(namedClass.outer) + "." + namedClass.name
        else -> throw IllegalArgumentException(namedClass.toString())
    }
}

fun NullabilityAnnotation.suffix(): String = if (this == NullabilityAnnotation.NULLABLE) "?" else ""

