package org.jetbrains.kannotator.nullability

public trait NullabilityValueInfo {
    val mayBeNull: Boolean
    val mayBeNotNull: Boolean
}

val NullabilityValueInfo.definitelyNull: Boolean
    get() = mayBeNull && !mayBeNotNull

val NullabilityValueInfo.definitelyNotNull: Boolean
    get() = mayBeNotNull && !mayBeNull

