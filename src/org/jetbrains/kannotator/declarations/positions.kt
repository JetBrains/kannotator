package org.jetbrains.kannotator.declarations

import java.util.ArrayList

class PositionsForMethod(val method: Method) {
    public fun get(positionWithinMethod: PositionWithinDeclaration): AnnotatedType {
        return AnnotatedTypeImpl(
                MethodTypePositionImpl(method, positionWithinMethod),
                positionWithinMethod.toString(),
                listOf()
        )
    }

    // If present, 'this' has index 0
    public fun forParameter(parameterIndex: Int): AnnotatedType{
        assert(parameterIndex >= 0) {"For return type use forReturnType() method"}
        return get(ParameterPosition(parameterIndex))
    }

    public fun forReturnType(): AnnotatedType = get(RETURN_TYPE)
}

public fun getFieldTypePosition(field: Field) : FieldTypePosition = FieldTypePositionImpl(field)

public fun getFieldAnnotatedType(field: Field) : AnnotatedType {
    return AnnotatedTypeImpl(FieldTypePositionImpl(field), "Field annotation type", listOf())
}

fun PositionsForMethod.forEachValidPosition(body: (AnnotationPosition) -> Unit) {
    val skip = if (method.isStatic()) 0 else 1
    for (i in skip..method.getArgumentTypes().size) {
        body(forParameter(i).position)
    }
    body(forReturnType().position)
}

fun PositionsForMethod.getValidPositions(): Collection<AnnotationPosition> {
    val result = ArrayList<AnnotationPosition>()
    forEachValidPosition {result.add(it)}
    return result
}

private data class MethodTypePositionImpl(
        override val method: Method,
        override val relativePosition: PositionWithinDeclaration
) : MethodTypePosition {
    override val member: ClassMember get() { return method }
}

private data class FieldTypePositionImpl(override val field: Field): FieldTypePosition {
    override val member: ClassMember get() { return field }
    override val relativePosition: PositionWithinDeclaration = FIELD_TYPE
}

private data class AnnotatedTypeImpl(
        override val position: AnnotationPosition,
        val debugName: String,
        override val arguments: List<AnnotatedType>
) : AnnotatedType {
    override fun toString(): String {
        val argStr =
                if (!arguments.isEmpty())
                    "<${arguments.joinToString(", ")}>"
                else ""
        return debugName + argStr
    }
}
