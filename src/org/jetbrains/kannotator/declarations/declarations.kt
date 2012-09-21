package org.jetbrains.kannotator.declarations

import org.objectweb.asm.Type

public data class Method(
    public val declaringClass: Type,
    public val method: org.objectweb.asm.commons.Method
) {
    public fun toString(): String {
        return declaringClass.getClassName() + ":" + method;
    }
}