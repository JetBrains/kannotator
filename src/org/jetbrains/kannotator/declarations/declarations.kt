package org.jetbrains.kannotator.declarations

import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import kotlinlib.suffixAfter
import kotlinlib.suffixAfterLast
import kotlinlib.buildString
import java.util.ArrayList

trait ClassMember {
    val declaringClass: ClassName
    val access: Access
    val name: String
}

data class MethodId(
        val methodName: String,
        val methodDesc: String
) {
    public fun toString(): String = methodName + methodDesc
}

fun MethodId.getReturnType(): Type = Type.getReturnType(methodDesc)
fun MethodId.getArgumentTypes(): Array<Type> = Type.getArgumentTypes(methodDesc) as Array<Type> //after KT-2872 should return Array<out Type>
fun MethodId.getSignatureDescriptor(): String {
    return methodName + methodDesc.substring(0, methodDesc.lastIndexOf(')'))
}

enum class Visibility {
    PUBLIC
    PROTECTED
    PACKAGE
    PRIVATE
}

data class Access(val flags: Int) {
    fun has(val flag: Int) = flags and flag != 0
    fun toString(): String = "" + Integer.toHexString(flags)
}

fun Method(
        declaringClass: ClassName,
        access: Int,
        name: String,
        desc: String,
        signature: String? = null
): Method = Method(declaringClass, Access(access), MethodId(name, desc), signature)

data class Method(
        override val declaringClass: ClassName,
        override val access: Access,
        val id: MethodId,
        val genericSignature: String? = null) : ClassMember {

    private var _parameterNames : List<String>? = null
    public fun setParameterNames(names: List<String>) {
        if (_parameterNames != null) {
            throw IllegalStateException("Parameter names already initialized: $parameterNames")
        }
        val arity = getArgumentTypes().size - if (isInnerClassConstructor()) 1 else 0
        if (names.size != arity) {
            throw IllegalArgumentException("Incorrect number of parameter names: $names, must be $arity")
        }
        _parameterNames = ArrayList(names)
    }

    public val parameterNames: List<String>
        get() {
            if (_parameterNames == null) {
                _parameterNames = defaultMethodParameterNames(this)
            }
            return _parameterNames!!
        }

    override val name: String
        get() = id.methodName

    public fun toString(): String {
        return declaringClass.toType().getClassName() + ":" + id.methodName + id.methodDesc;
    }
}

private fun defaultMethodParameterNames(method: Method): List<String>
        = (0..method.getArgumentTypes().size - 1).map { i -> "p$i" }

fun Method.getReturnType(): Type = id.getReturnType()
fun Method.getArgumentTypes(): Array<out Type> = id.getArgumentTypes()

fun ClassMember.isStatic(): Boolean = access.has(Opcodes.ACC_STATIC)

fun ClassMember.isFinal(): Boolean = access.has(Opcodes.ACC_FINAL)

fun Method.isConstructor(): Boolean = name == "<init>"

fun Method.isInnerClassConstructor(): Boolean {
    if (!isConstructor()) return false

    val parameterTypes = getArgumentTypes()
    if (parameterTypes.size == 0) return false

    val firstParameter = parameterTypes[0]
    if (firstParameter.getSort() != Type.OBJECT) return false

    val dollarIndex = declaringClass.internal.lastIndexOf('$')
    if (dollarIndex < 0) return false
    val outerClass = declaringClass.internal.substring(0, dollarIndex)

    return firstParameter.getInternalName() == outerClass
}

val ClassMember.visibility: Visibility get() = when {
    access.has(Opcodes.ACC_PUBLIC) -> Visibility.PUBLIC
    access.has(Opcodes.ACC_PROTECTED) -> Visibility.PROTECTED
    access.has(Opcodes.ACC_PRIVATE) -> Visibility.PRIVATE
    else -> Visibility.PACKAGE
}

fun Method.isVarargs(): Boolean = access.has(Opcodes.ACC_VARARGS)

fun Method.toFullString(): String {
    return buildString {
        it.append(visibility.toString().toLowerCase() + " ")
        it.append(if (isStatic()) "static " else "")
        it.append(if (isFinal()) "final " else "")
        it.append("flags[$access] ")
        it.append(declaringClass.internal)
        it.append("::")
        it.append(id.methodName)
        it.append(id.methodDesc)
        if (genericSignature != null) {
            it.append(" :: ")
            it.append(genericSignature)
        }
    }
}

data class ClassName private (val internal: String) {
    val typeDescriptor: String
        get() = "L$internal;"

    public fun toString(): String = internal

    val simple: String
        get() = canonicalName.suffixAfterLast(".")

    class object {
        fun fromInternalName(name: String): ClassName {
            return ClassName(name)
        }

        fun fromType(_type: Type): ClassName {
            return ClassName.fromInternalName(_type.getInternalName())
        }
    }
}

val ClassName.canonicalName: String
    get() = internal.internalNameToCanonical()

fun String.internalNameToCanonical(): String = replace('/', '.').toCanonical()

fun String.toCanonical(): String = replace('$', '.')

fun ClassName.toType(): Type {
    return Type.getType(typeDescriptor)
}

fun ClassName.isAnonymous(): Boolean {
    // simple name consist of digits only
    for (c in simple) {
        if (!c.isDigit()) {
            return false
        }
    }
    return true
}

data class FieldId(val fieldName: String) {
    public fun toString(): String = fieldName
}

fun Field(declaringClass: ClassName,
          access: Int,
          name: String,
          desc: String,
          signature: String? = null,
          value: Any? = null): Field = Field(declaringClass, Access(access), FieldId(name), desc, signature, value)

data class Field(
        override val declaringClass: ClassName,
        override val access: Access,
        val id: FieldId,
        desc: String,
        val genericSignature: String? = null,
        value: Any? = null) : ClassMember {

    override val name = id.fieldName

    public val value : Any? = value
    public val desc : String = desc

    public fun toString(): String {
        return declaringClass.toType().getClassName() + ":" + id.fieldName;
    }
}

fun Field.getType(): Type = Type.getReturnType(desc)