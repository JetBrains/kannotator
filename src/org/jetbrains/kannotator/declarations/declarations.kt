package org.jetbrains.kannotator.declarations

import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import java.util.ArrayList
import org.objectweb.asm.tree.MethodNode

data class Package(val name: String) {
    override fun toString() = name
}

interface ClassMember {
    val declaringClass: ClassName
    val access: Access
    val name: String
}

data class MethodId(
        val methodName: String,
        val methodDesc: String
) {
    override fun toString() = methodName + methodDesc
}

fun MethodId.getReturnType(): Type = Type.getReturnType(methodDesc)
fun MethodId.getArgumentTypes(): Array<Type> = Type.getArgumentTypes(methodDesc)
/**
 * for method  findClass(Ljava/lang/String;)Ljava/lang/Class; it will return a piece of its signature: findClass(Ljava/lang/String;
 */
fun MethodId.getSignatureDescriptor(): String {
    return methodName + methodDesc.substring(0, methodDesc.lastIndexOf(')'))
}

enum class Visibility {
    PUBLIC,
    PROTECTED,
    PACKAGE,
    PRIVATE
}

data class Access(val flags: Int) {
    fun has(flag: Int) = flags and flag != 0
    override fun toString() = "" + Integer.toHexString(flags)
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
        val arity = getArgumentTypes().size
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

    override fun toString(): String {
        return declaringClass.toType().className + ":" + id.methodName + id.methodDesc;
    }
}

fun Method(className: ClassName, methodNode: MethodNode): Method = Method(
        className, methodNode.access, methodNode.name, methodNode.desc, methodNode.signature)

private fun defaultMethodParameterNames(method: Method): List<String>
        = (0..method.getArgumentTypes().size - 1).toList().map { i -> "p$i" }

fun Method.getReturnType(): Type = id.getReturnType()
fun Method.getArgumentTypes(): Array<out Type> = id.getArgumentTypes()

fun Access.isStatic(): Boolean = has(Opcodes.ACC_STATIC)
fun Access.isFinal(): Boolean = has(Opcodes.ACC_FINAL)
fun Access.isPrivate(): Boolean = has(Opcodes.ACC_PRIVATE)
fun Access.isProtected(): Boolean = has(Opcodes.ACC_PROTECTED)
fun Access.isPublic(): Boolean = has(Opcodes.ACC_PUBLIC)
fun Access.isPublicOrProtected(): Boolean = isPublic() || isProtected()

fun ClassMember.isStatic(): Boolean = access.isStatic()
fun ClassMember.isFinal(): Boolean = access.isFinal()
fun ClassMember.isPublicOrProtected(): Boolean = access.isPublicOrProtected()
fun ClassMember.isPrivate(): Boolean = access.isPrivate()

fun ClassDeclaration.isPublic(): Boolean = access.isPublic()

fun Method.isConstructor(): Boolean = id.methodName == "<init>"
fun Method.isClassInitializer(): Boolean = id.methodName == "<clinit>"

fun Method.isInnerClassConstructor(): Boolean {
    if (!isConstructor()) return false

    val parameterTypes = getArgumentTypes()
    if (parameterTypes.size == 0) return false

    val firstParameter = parameterTypes[0]
    if (firstParameter.sort != Type.OBJECT) return false

    val dollarIndex = declaringClass.internal.lastIndexOf('$')
    if (dollarIndex < 0) return false
    val outerClass = declaringClass.internal.substring(0, dollarIndex)

    return firstParameter.internalName == outerClass
}

val ClassMember.visibility: Visibility get() = when {
    access.has(Opcodes.ACC_PUBLIC) -> Visibility.PUBLIC
    access.has(Opcodes.ACC_PROTECTED) -> Visibility.PROTECTED
    access.has(Opcodes.ACC_PRIVATE) -> Visibility.PRIVATE
    else -> Visibility.PACKAGE
}

fun Method.isVarargs(): Boolean = access.has(Opcodes.ACC_VARARGS)

fun Method.toFullString(): String {
    return StringBuilder().apply {
        append(visibility.toString().toLowerCase() + " ")
        append(if (isStatic()) "static " else "")
        append(if (isFinal()) "final " else "")
        append("flags[$access] ")
        append(declaringClass.internal)
        append("::")
        append(id.methodName)
        append(id.methodDesc)
        if (genericSignature != null) {
            append(" :: ")
            append(genericSignature)
        }
    }.toString()
}

data class ClassName private constructor(val internal: String) {
    val typeDescriptor: String
        get() = "L$internal;"

    override fun toString() = internal

    val simple: String
        get() = canonicalName.substring(canonicalName.lastIndexOf(".").let { if (it == -1) 0 else it + 1 })

    companion object {
        fun fromInternalName(name: String): ClassName {
            return ClassName(name)
        }

        fun fromType(_type: Type): ClassName {
            return ClassName.fromInternalName(_type.internalName)
        }
    }
}

data class ClassDeclaration(val className: ClassName, val access: Access)

val ClassName.canonicalName: String
    get() = internal.internalNameToCanonical()

fun String.internalNameToCanonical(): String = replace('/', '.').toCanonical()

fun String.toCanonical(): String {
    //keep last $ in class name: it's generated in scala bytecode
    val lastCharIndex = this.length - 1
    return this.substring(0, lastCharIndex).replace('$', '.') + this.substring(lastCharIndex)
}

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

val ClassName.packageName: String
    get() = internal.substringBeforeLast('/')

val ClassMember.packageName: String
    get() = declaringClass.packageName

data class FieldId(val fieldName: String) {
    override fun toString() = fieldName
}

fun Field(declaringClass: ClassName,
          access: Int,
          name: String,
          desc: String,
          signature: String? = null,
          value: Any? = null): Field = Field(declaringClass, Access(access), FieldId(name), desc, signature, value)

class Field(
        override val declaringClass: ClassName,
        override val access: Access,
        val id: FieldId,
        desc: String,
        val genericSignature: String? = null,
        value: Any? = null) : ClassMember {

    override fun equals(other: Any?) =
            other is Field && declaringClass == other.declaringClass && access == other.access &&
            id == other.id && genericSignature == other.genericSignature

    override fun hashCode(): Int {
        var hashCode = 5
        hashCode = hashCode * 17 + declaringClass.hashCode()
        hashCode = hashCode * 17 + access.hashCode()
        hashCode = hashCode * 17 + id.hashCode()
        hashCode = hashCode * 17 + (genericSignature?.hashCode() ?: 0)
        return hashCode
    }

    override val name = id.fieldName

    public val value : Any? = value
    public val desc : String = desc

    override fun toString(): String {
        return declaringClass.toType().className + ":" + id.fieldName;
    }
}

fun Field.getType(): Type = Type.getReturnType(desc)

fun ClassMember.getInternalPackageName(): String {
    val className = declaringClass.internal
    val delimiter = className.lastIndexOf('/')
    return if (delimiter >= 0) className.substring(0, delimiter) else ""
}