package org.jetbrains.kannotator.declarations

import org.objectweb.asm.Type
import org.objectweb.asm.commons.Method as AsmMethod

public class Method(
    public val declaringClass: Type,
    public val asmMethod: AsmMethod
) {
    public fun toString(): String {
        return declaringClass.getClassName() + ":" + asmMethod;
    }
}