package org.jetbrains.kannotator.declarations

import java.util.ArrayList
import org.objectweb.asm.signature.SignatureReader
import org.objectweb.asm.signature.SignatureVisitor
import org.objectweb.asm.Opcodes.*
import kotlinlib.join
import java.util.Collections

class Positions(val method: Method) {
    // If present, 'this' has index 0
    public fun forParameter(parameterIndex: Int): AnnotatedType{
        assert(parameterIndex >= 0) {"For return type use forReturnType() method"}
        return AnnotatedTypeImpl(
                TypePositionImpl(method, parameterIndex, 0),
                "param$parameterIndex",
                ArrayList(0)
        )
    }

    public fun forReturnType() : AnnotatedType = AnnotatedTypeImpl(
            TypePositionImpl(method, -1, 0),
            "return",
            ArrayList(0)
    )

}

private data class TypePositionImpl(
        val method: Method,
        val parameterIndex: Int, // -1 for return type, 0 for 'this' if present
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
