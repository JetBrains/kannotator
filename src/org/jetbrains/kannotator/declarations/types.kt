package org.jetbrains.kannotator.declarations

import kotlinlib.join

data class TypeID(
        val method: Method,
        val internalTypeName: String,
        val position: Int // position from left to right inside the signaturem or in the descriptor if signature is absent
)

class AnnotatedType(
        val id: TypeID,
        val arguments: List<AnnotatedType>
) {
     fun toString(): String {
         val argStr =
            if (!arguments.isEmpty())
                "<${arguments.join(", ")}>"
            else ""
         return "$id" + argStr
     }
}