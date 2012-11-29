package org.jetbrains.kannotator.kotlinSignatures

import org.objectweb.asm.signature.SignatureVisitor
import java.util.ArrayList
import org.objectweb.asm.Opcodes
import org.objectweb.asm.signature.SignatureReader

trait Classifier
class BaseType(val descriptor: Char) : Classifier
trait NamedClass : Classifier
class ToplevelClass(val internalName: String) : NamedClass
class InnerClass(val outer: GenericType, val name: String) : NamedClass
class TypeVariable(val name: String) : Classifier
object Array : Classifier

enum class Wildcard {
    SUPER   // ? super X
    EXTENDS // ? extends X
}

trait TypeArgument
class BoundedWildcard(val wildcard: Wildcard, val bound: GenericType) : TypeArgument
object UnBoundedWildcard : TypeArgument
class NoWildcard(val genericType: GenericType) : TypeArgument

trait GenericType {
    val classifier: Classifier
    val arguments: List<TypeArgument>
}

class ImmutableGenericType(
    override val classifier: Classifier,
    override val arguments: List<TypeArgument>
) : GenericType

val GenericType.arrayElementType: GenericType
    get() {
        assert(arguments.size == 1)
        assert(classifier == Array)
        return (arguments[0] as NoWildcard).genericType
    }

class GenericTypeImpl : GenericType {
    var classifierVar: Classifier? = null
    override val arguments: MutableList<TypeArgument> = ArrayList()
    override val classifier: Classifier
        get() = classifierVar!!
}

class TypeParameter(val name: String, val upperBounds: List<GenericType>)
class ValueParameter(val index: Int, val genericType: GenericType)

class GenericMethodSignature(
    val typeParameters: List<TypeParameter>,
    val returnType: GenericType,
    val valueParameters: List<ValueParameter>
)

fun TypeParameter.hasNontrivialBounds(): Boolean {
    assert(upperBounds.size > 0)
    if (upperBounds.size > 1) return true
    val bound = upperBounds[0].classifier
    return !(bound is ToplevelClass && bound.internalName == "java/lang/Object")
}

fun parseGenericMethodSignature(signature: String): GenericMethodSignature {
    val typeParameters = ArrayList<TypeParameter>()
    val returnType = GenericTypeImpl()
    val valueParameters = ArrayList<ValueParameter>()

    SignatureReader(signature).accept(
            object : SignatureVisitor(Opcodes.ASM4) {
                var bounds = ArrayList<GenericType>()

                public override fun visitFormalTypeParameter(name: String?) {
                    bounds = ArrayList<GenericType>()
                    var param = TypeParameter(name!!, bounds)
                    typeParameters.add(param)
                }

                public override fun visitClassBound(): SignatureVisitor {
                    val bound = GenericTypeImpl()
                    bounds.add(bound)
                    return GenericTypeParser(bound)
                }

                public override fun visitInterfaceBound(): SignatureVisitor {
                    val bound = GenericTypeImpl()
                    bounds.add(bound)
                    return GenericTypeParser(bound)
                }

                public override fun visitParameterType(): SignatureVisitor {
                    val parameterType = GenericTypeImpl()
                    val param = ValueParameter(valueParameters.size(), parameterType)
                    valueParameters.add(param)
                    return GenericTypeParser(parameterType)
                }

                public override fun visitReturnType(): SignatureVisitor {
                    return GenericTypeParser(returnType)
                }
            }
    )
    return GenericMethodSignature(typeParameters, returnType, valueParameters)
}

private class GenericTypeParser(private var result: GenericTypeImpl) : SignatureVisitor(Opcodes.ASM4) {

    override fun visitBaseType(descriptor: Char) {
        result.classifierVar = BaseType(descriptor)
    }

    override fun visitTypeVariable(name: String) {
        result.classifierVar = TypeVariable(name)
    }

    override fun visitArrayType(): SignatureVisitor {
        result.classifierVar = Array
        val argument = GenericTypeImpl()
        result.arguments.add(NoWildcard(argument))
        return GenericTypeParser(argument)
    }

    override fun visitClassType(name: String) {
        result.classifierVar = ToplevelClass(name)
    }

    override fun visitInnerClassType(name: String) {
        val inner = GenericTypeImpl()
        inner.classifierVar = InnerClass(result, name)
        result = inner
    }

    override fun visitTypeArgument() {
        result.arguments.add(UnBoundedWildcard)
    }

    override fun visitTypeArgument(wildcard: Char): SignatureVisitor {
        val argument = GenericTypeImpl()
        result.arguments.add(when (wildcard) {
            SignatureVisitor.EXTENDS -> BoundedWildcard(Wildcard.EXTENDS, argument)
            SignatureVisitor.SUPER -> BoundedWildcard(Wildcard.SUPER, argument)
            SignatureVisitor.INSTANCEOF -> NoWildcard(argument)
            else -> throw IllegalArgumentException("Unkonown wildcard: $wildcard")
        })
        return GenericTypeParser(argument)
    }
}
