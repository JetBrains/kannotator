package org.jetbrains.kannotator.kotlinSignatures

import org.jetbrains.kannotator.annotationsInference.nullability.NullabilityAnnotation
import org.jetbrains.kannotator.declarations.Annotations
import org.jetbrains.kannotator.annotations.io.AnnotationData
import org.jetbrains.kannotator.declarations.Method
import org.jetbrains.kannotator.declarations.Field
import org.jetbrains.kannotator.annotations.io.AnnotationDataImpl
import org.jetbrains.kannotator.declarations.getArgumentTypes
import org.objectweb.asm.Type
import kotlinlib.*
import org.jetbrains.kannotator.annotations.io.getMethodNameAccountingForConstructor
import org.objectweb.asm.Opcodes
import org.jetbrains.kannotator.declarations.isFinal
import org.jetbrains.kannotator.declarations.getType
import org.jetbrains.kannotator.declarations.PositionsForMethod
import org.jetbrains.kannotator.declarations.getFieldTypePosition
import org.jetbrains.kannotator.declarations.isStatic
import java.util.ArrayList
import org.jetbrains.kannotator.declarations.isConstructor
import org.jetbrains.kannotator.index.NO_PARAMETER_NAME
import org.jetbrains.kannotator.controlFlow.builder.analysis.mutability.MutabilityAnnotation

fun kotlinSignatureToAnnotationData(kotlinSignatureString: String): AnnotationData {
    return AnnotationDataImpl("jet.runtime.typeinfo.KotlinSignature", hashMapOf("value" to "\"$kotlinSignatureString\""))
}

private data class KnownAnnotations(
        val nullability: NullabilityAnnotation,
        val mutability: MutabilityAnnotation
)

val NULLABLE_READONLY = KnownAnnotations(NullabilityAnnotation.NULLABLE, MutabilityAnnotation.READ_ONLY)
val NULLABLE_MUTABLE = KnownAnnotations(NullabilityAnnotation.NULLABLE, MutabilityAnnotation.MUTABLE)

fun renderMethodSignature(
        method: Method,
        nullability: Annotations<NullabilityAnnotation>,
        mutability: Annotations<MutabilityAnnotation>
): String {
    val signature = parseGenericMethodSignature(method.genericSignature ?: method.id.methodDesc)

    val typeParametersByName = signature.typeParameters.toMap { it.name }

    val isConstructor = method.isConstructor()
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
                    return renderType(bound, Position.UPPER_BOUND, NULLABLE_READONLY)
                }
                if (param.upperBounds.size() == 1) {
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

    // Enum constructors have two extra parameters in the desc: (Ljava/lang/String;I)
    // and, of course, these parameters have no names
    val paramShift = if (method.genericSignature == null) 0 else method.getArgumentTypes().size() - signature.valueParameters.size()

    var commaBefore = false
    for (param in signature.valueParameters) {
        val index = param.index + paramShift
        sb.appendParameter(method, index, commaBefore) {
            vararg ->
            val annotations = method.getAnnotationsForParameter(nullability, mutability, index, NULLABLE_READONLY)
            if (vararg) {
                sb.append(renderType(substituteIfNeeded(param.genericType.arrayElementType), Position.VARARG, annotations))
            }
            else {
                sb.append(renderType(substituteIfNeeded(param.genericType), Position.METHOD_PARAMETER, annotations))
            }
            commaBefore = true
        }
    }

    if (isConstructor) {
        // Constructor
        sb.append(")")
    }
    else {
        sb.append(") : ")
        val returnType = signature.returnType
        val annotations = method.getAnnotationsForReturnType(nullability, mutability, NULLABLE_MUTABLE)
        sb.append(renderType(returnType, Position.RETURN_TYPE, annotations))
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
    val pos = getFieldTypePosition(field)
    val annotations = KnownAnnotations(
            nullability[pos] ?: NullabilityAnnotation.NULLABLE,
            mutability[pos] ?: MutabilityAnnotation.MUTABLE
    )

    sb.append(renderType(field.getType().toGenericType(), Position.RETURN_TYPE, annotations))
    return sb.toString()
}

fun Type.toGenericType(): GenericType {
    val signature = "(${this.getDescriptor()})V"
    return parseGenericMethodSignature(signature).valueParameters[0].genericType
}

fun Method.getAnnotationsForParameter(
        nullability: Annotations<NullabilityAnnotation>,
        mutability: Annotations<MutabilityAnnotation>,
        parameterIndex: Int, default: KnownAnnotations): KnownAnnotations {
    return KnownAnnotations(
            nullability.getAnnotationForParameter(this, parameterIndex, default.nullability),
            mutability.getAnnotationForParameter(this, parameterIndex, default.mutability)
    )
}

fun Method.getAnnotationsForReturnType(
        nullability: Annotations<NullabilityAnnotation>,
        mutability: Annotations<MutabilityAnnotation>,
        default: KnownAnnotations): KnownAnnotations {
    return KnownAnnotations(
            nullability.getAnnotationForReturnType(this, default.nullability),
            mutability.getAnnotationForReturnType(this, default.mutability)
    )
}

fun <A : Any> Annotations<A>.getAnnotationForParameter(method: Method, parameterIndex: Int, default: A): A {
    val thisOffset = if (method.isStatic()) 0 else 1
    val positions = PositionsForMethod(method)
    val annotationPosition = positions.forParameter(parameterIndex + thisOffset).position
    return this[annotationPosition] ?: default
}

fun <A : Any> Annotations<A>.getAnnotationForReturnType(method: Method, default: A): A {
    val positions = PositionsForMethod(method)
    val annotationPosition = positions.forReturnType().position
    return this[annotationPosition] ?: default
}

fun StringBuilder.appendParameter(method: Method, parameterIndex: Int, commaBefore: Boolean, forType: (isVararg: Boolean) -> Unit) {
    val name = method.parameterNames[parameterIndex]
    if (name == NO_PARAMETER_NAME) return

    if (commaBefore) {
        append(", ")
    }

    val last = parameterIndex == method.parameterNames.size() - 1
    val vararg = last && method.access.has(Opcodes.ACC_VARARGS)
    if (vararg) {
        append("vararg ")
    }
    append(name)
    append(" : ")
    forType(vararg)
}

enum class Position {
    METHOD_PARAMETER,
    RETURN_TYPE,
    VARARG,
    CLASS_TYPE_ARGUMENT,
    UPPER_BOUND,
    OUTER
}

fun renderType(genericType: GenericType, position: Position, annotations: KnownAnnotations): String {
    val classifier = genericType.classifier
    val suffix = if (position == Position.OUTER) "" else annotations.nullability.suffix()
    return when (classifier) {
        is BaseType -> renderBaseType(classifier)
        is NamedClass -> renderNamedClass(classifier, annotations) + renderArguments(genericType, position) + suffix
        is TypeVariable -> renderTypeVariable(classifier, position, annotations)
        Array -> renderArrayType(genericType, position) + suffix
        else -> throw IllegalArgumentException("Unknown classifier: $classifier")
    }
}

fun renderArguments(genericType: GenericType, position: Position): String {
    if (genericType.arguments.isEmpty()) return ""
    return StringBuilder {
        append("<")
        for ((i, arg) in genericType.arguments.indexed) {
            if (i > 0) {
                append(", ")
            }
            append(when (arg) {
                UnBoundedWildcard -> "*"
                is BoundedWildcard -> arg.wildcard.projection() + renderType(arg.bound, Position.UPPER_BOUND, NULLABLE_READONLY)
                is NoWildcard -> renderType(arg.genericType, Position.CLASS_TYPE_ARGUMENT, NULLABLE_READONLY)
                else -> throw IllegalStateException(arg.toString())
            })
        }
        append(">")
    }.toString()
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

fun renderTypeVariable(variable: TypeVariable, position: Position, annotations: KnownAnnotations): String {
    val nullableByDefault = position !in listOf(Position.CLASS_TYPE_ARGUMENT, Position.UPPER_BOUND)
    return variable.name + if (nullableByDefault) annotations.nullability.suffix() else ""
}

fun renderArrayType(arrayType: GenericType, position: Position): String {
    val elementType = arrayType.arrayElementType
    if (elementType.classifier is BaseType) {
        return renderBaseArrayType(elementType.classifier as BaseType)

    }
    return "Array<${arrayElementPosition(position)}${renderType(elementType, Position.CLASS_TYPE_ARGUMENT, NULLABLE_READONLY)}>"
}

fun arrayElementPosition(position: Position): String = if (position != Position.VARARG) "out " else ""

fun renderNamedClass(namedClass: NamedClass, annotations: KnownAnnotations): String {
    fun prefix(s: String): String = annotations.mutability.prefix() + s

    return when (namedClass) {
        is ToplevelClass -> when (namedClass.internalName) {
            "java/lang/Object" -> "Any"
            "java/lang/Integer" -> "Int"
            "java/lang/Iterable",
            "java/util/Iterator",
            "java/util/Collection",
            "java/util/List",
            "java/util/ListIterator",
            "java/util/Set",
            "java/util/Map" -> prefix(namedClass.internalName.substringAfterLast("/"))
            "java/util/Map\$Entry" -> prefix("Map") + "." + prefix("Entry")
            else -> namedClass.internalName.substringAfterLast("/").replace('$', '.')
        }
        is InnerClass -> renderType(namedClass.outer, Position.OUTER, annotations) + "." + namedClass.name
        else -> throw IllegalArgumentException(namedClass.toString())
    }
}

fun NullabilityAnnotation.suffix(): String = if (this == NullabilityAnnotation.NULLABLE) "?" else ""
fun MutabilityAnnotation.prefix(): String = if (this == MutabilityAnnotation.MUTABLE) "Mutable" else ""
