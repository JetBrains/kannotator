package org.jetbrains.kannotator.annotations.io

import java.io.Reader
import org.jetbrains.kannotator.declarations.TypePosition
import org.jetbrains.kannotator.declarations.ClassName
import org.jetbrains.kannotator.declarations.Method
import org.jetbrains.kannotator.declarations.MethodId
import org.objectweb.asm.Type

data class AnnotationData(val annotationClassFqn: String, attributes: Map<String, String>)

trait AnnotationParserHelper {
    fun getClassName(canonicalName: String): ClassName?
    fun getMethodById(className: ClassName, methodId: MethodId): Method?
    fun handler(position: TypePosition, data: AnnotationData): Unit

}

fun parseAnnotations(
        xml: Reader,
        helper: AnnotationParserHelper
) {

}

data class TypePositionInfo(
        val classCanonicalName: String,
        val methodName: String,
        val parameterTypeStrings: List<String>,
        val returnTypeString: String?,
        val position: Int?)

fun parseItemName(itemName: String): TypePositionInfo {
    throw UnsupportedOperationException()
}

fun typeStringToType(typeString: String, helper: AnnotationParserHelper): Type {
    throw UnsupportedOperationException()
}

fun TypePositionInfo.toMethodId(helper: AnnotationParserHelper): MethodId {
    throw UnsupportedOperationException()
}