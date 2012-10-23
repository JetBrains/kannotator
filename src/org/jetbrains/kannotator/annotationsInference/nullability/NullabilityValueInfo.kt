package org.jetbrains.kannotator.annotationsInference.nullability

public enum class NullabilityValueInfo {
    NOT_NULL
    NULL
    NULLABLE
    CONFLICT
    UNKNOWN

    // can be cached
    fun merge(that: NullabilityValueInfo): NullabilityValueInfo {
        if (this == that) return this

        if (this == CONFLICT) return that
        if (that == CONFLICT) return this

        if (this == NULL || that == NULL) return NULLABLE

        if (this == NULLABLE || that == NULLABLE) return NULLABLE

        return UNKNOWN // NOT_NULL + UNKNOWN
    }
}

fun Iterable<NullabilityValueInfo>.merge() : NullabilityValueInfo {
    if (!iterator().hasNext()) return NullabilityValueInfo.UNKNOWN
    return this.fold(NullabilityValueInfo.CONFLICT, {
        (res : NullabilityValueInfo, value : NullabilityValueInfo) -> res merge value
    } )
}

fun NullabilityValueInfo.mergeWithNullable(that: NullabilityValueInfo?) =
        if (that == null) this else this.merge(that)