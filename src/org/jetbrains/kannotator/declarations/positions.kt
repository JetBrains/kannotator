package org.jetbrains.kannotator.declarations

import java.util.ArrayList
import org.objectweb.asm.signature.SignatureReader
import org.objectweb.asm.signature.SignatureVisitor
import org.objectweb.asm.Opcodes.*
import kotlinlib.join
import java.util.Collections

class PositionsWithinMember(val method: Method) {
    public fun get(positionWithinMethod: PositionWithinMethod): AnnotatedType {
        return AnnotatedTypeImpl(
                TypePositionImpl(method, positionWithinMethod, 0),
                positionWithinMethod.toString(),
                ArrayList(0)
        )
    }

    // If present, 'this' has index 0
    public fun forParameter(parameterIndex: Int): AnnotatedType{
        assert(parameterIndex >= 0) {"For return type use forReturnType() method"}
        return get(ParameterPosition(parameterIndex))
    }

    public fun forReturnType(): AnnotatedType = get(RETURN_TYPE)
}

fun PositionsWithinMember.forEachValidPosition(body: (TypePosition) -> Unit) {
    val skip = if (method.isStatic()) 0 else 1
    for (i in skip..method.getArgumentTypes().size) {
        body(forParameter(i).position)
    }
    body(forReturnType().position)
}

fun PositionsWithinMember.getValidPositions(): Collection<TypePosition> {
    val result = ArrayList<TypePosition>()
    forEachValidPosition {result.add(it)}
    return result
}

private data class TypePositionImpl(
        override val method: Method,
        override val positionWithinMethod: PositionWithinMethod,
        val position: Int // position from left to right inside the type signature
) : TypePosition

private data class AnnotatedTypeImpl(
        override val position: TypePosition,
        val debugName: String,
        override val arguments: MutableList<AnnotatedType>
) : AnnotatedType {
    fun toString(): String {
        val argStr =
                if (!arguments.isEmpty())
                    "<${arguments.join(", ")}>"
                else ""
        return debugName + argStr
    }
}
