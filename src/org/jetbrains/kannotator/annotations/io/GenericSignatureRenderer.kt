package org.jetbrains.kannotator.annotations.io

import org.objectweb.asm.signature.SignatureVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.signature.SignatureReader
import org.jetbrains.kannotator.declarations.toCanonical
import org.jetbrains.kannotator.declarations.internalNameToCanonical

fun renderMethodParameters(genericSignature: String): String {
    val renderer = GenericMethodParametersRenderer()
    SignatureReader(genericSignature).accept(renderer)
    return renderer.parameters()
}

fun renderReturnType(genericSignature: String): String {
    val sb = StringBuilder()
    SignatureReader(genericSignature).accept(object : SignatureVisitor(Opcodes.ASM4) {

        public override fun visitReturnType(): SignatureVisitor {
            return GenericTypeRenderer(sb)
        }
    })
    return sb.toString()
}

private class GenericMethodParametersRenderer: SignatureVisitor(Opcodes.ASM4) {

    private val sb = StringBuilder("(")
    private var first = true

    fun parameters(): String = sb.toString() + ")"

    override fun visitParameterType(): SignatureVisitor {
        if (first) {
            first = false
            // "(" is already appended
        }
        else {
            sb.append(", ")
        }
        return GenericTypeRenderer(sb)
    }
}

private open class GenericTypeRenderer(val sb: StringBuilder): SignatureVisitor(Opcodes.ASM4) {

    private var angleBracketOpen = false

    private fun openAngleBracketIfNeeded(): Boolean {
        if (!angleBracketOpen) {
            angleBracketOpen = true
            sb.append("<")
            return true
        }
        return false
    }

    private fun closeAngleBracketIfNeeded() {
        if (angleBracketOpen) {
            angleBracketOpen = false
            sb.append(">")
        }
    }

    private fun beforeTypeArgument() {
        val first = openAngleBracketIfNeeded()
        if (!first) sb.append(",")
    }

    protected open fun endType() {}

    override fun visitBaseType(descriptor: Char) {
        sb.append(when (descriptor) {
            'V' -> "void"
            'B' -> "byte"
            'J' -> "long"
            'Z' -> "boolean"
            'I' -> "int"
            'S' -> "short"
            'C' -> "char"
            'F' -> "float"
            'D' -> "double"
            else -> throw IllegalArgumentException("Unknown base type: $descriptor")
        })
        endType()
    }

    override fun visitTypeVariable(name: String) {
        sb.append(name)
        endType()
    }

    override fun visitArrayType(): SignatureVisitor {
        return object : GenericTypeRenderer(sb) {
            override fun endType() {
                sb.append("[]")
            }
        }
    }

    override fun visitClassType(name: String) {
        sb.append(name.internalNameToCanonical())
    }

    override fun visitInnerClassType(name: String) {
        closeAngleBracketIfNeeded()
        sb.append(".").append(name.internalNameToCanonical())

    }

    override fun visitTypeArgument() {
        beforeTypeArgument()
        sb.append("?")
    }

    override fun visitTypeArgument(wildcard: Char): SignatureVisitor {
        beforeTypeArgument()
        when (wildcard) {
            SignatureVisitor.EXTENDS -> sb.append("? extends ")
            SignatureVisitor.SUPER -> sb.append("? super ")
            SignatureVisitor.INSTANCEOF -> {}
            else -> IllegalArgumentException("Unkonown wildcard: $wildcard")
        }
        return GenericTypeRenderer(sb)
    }

    override fun visitEnd() {
        closeAngleBracketIfNeeded()
        endType()
    }
}