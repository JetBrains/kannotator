package org.jetbrains.kannotator.controlFlow.builder.analysis

import org.objectweb.asm.*
import org.objectweb.asm.tree.*
import java.util.HashSet
import org.objectweb.asm.tree.analysis.Value

val UNDEFINED_TYPE = Type.getType("U")
val PRIMITIVE_TYPE_SIZE_1 = Type.getType("P1")
val PRIMITIVE_TYPE_SIZE_2 = Type.getType("P2")
val NULL_TYPE = Type.getType("null")

public class TypedValue(
        val id: Int,
        val _type: Type,
        val parameterIndex: Int?,
        val createdAt: AbstractInsnNode?
) {
    override fun toString(): String {
        val s = "$_type#$id"
        return if (parameterIndex != null)
            "$parameterIndex!$s"
        else s
    }
}

public class QualifiedValue<out Q: Qualifier>(val base: TypedValue, val qualifier: Q) {
    override fun toString() = "$base|$qualifier"

    fun <T: Qualifier> copy(newQualifier: T): QualifiedValue<T> {
        return if (newQualifier != qualifier) {
            QualifiedValue(base, newQualifier)
        }
        else this as QualifiedValue<T>
    }
}

val TypedValue.size: Int
    get() = when (_type) {
        UNDEFINED_TYPE -> 1
        PRIMITIVE_TYPE_SIZE_2 -> 2
        else -> _type.size
    }

val TypedValue.interesting: Boolean
    get() = parameterIndex != null

interface CopyableValue<out V: CopyableValue<V>>: Value {
    public fun copy(): V
}

fun <Q: Qualifier> qualifiedValueSetOf(value: QualifiedValue<Q>) = QualifiedValueSet<Q>(value.base.size, hashSetOf(value))

fun <Q: Qualifier> qualifiedValueSetOf(vararg values: QualifiedValue<Q>): QualifiedValueSet<Q> {
    if (values.isEmpty()) {
        return QualifiedValueSet<Q>(1, hashSetOf())
    }
    val size = values[0].base.size
    for (value in values) {
        if (value.base.size != size) throw IllegalStateException("Inconsistent sizes: ${values.toList()}")
    }
    return QualifiedValueSet<Q>(size, values.toSet())
}

public class QualifiedValueSet<out Q: Qualifier>(
        val _size: Int, val values: Set<QualifiedValue<Q>>
) : CopyableValue<QualifiedValueSet<Q>> {
    public override fun getSize(): Int = _size

    public override fun toString(): String = values.toString()

    public override fun copy(): QualifiedValueSet<Q> = QualifiedValueSet<Q>(_size, HashSet(values))
}