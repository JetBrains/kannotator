package org.jetbrains.kannotator.annotations.io

import java.util.regex.Pattern
import kotlinlib.*

data class MethodAnnotationKeyData(
        val canonicalClassName: String,
        val returnType: String,
        val methodName: String
)

data class FieldAnnotationKeyData(
        val canonicalClassName: String,
        val fieldName: String
)

private val METHOD_ANNOTATION_KEY_PATTERN = Pattern.compile("""([\w\.]+) (.*?)\s?([\w<>$]+)\(""")
private val FIELD_ANNOTATION_KEY_PATTERN = Pattern.compile("""([\w\.]+) ([\w$]+)""")

fun parseMethodAnnotationKey(annotationKey: String): MethodAnnotationKeyData {
    val matcher = METHOD_ANNOTATION_KEY_PATTERN.matcher(annotationKey)
    if (!matcher.find()) {
        throw IllegalArgumentException("Can't parse annotation key $annotationKey")
    }
    return MethodAnnotationKeyData(matcher[1]!!, matcher[2]!!, matcher[3]!!)
}

fun parseFieldAnnotationKey(annotationKey: String): FieldAnnotationKeyData {
    val matcher = FIELD_ANNOTATION_KEY_PATTERN.matcher(annotationKey)
    if (!matcher.find()) {
        throw IllegalArgumentException("Can't parse annotation key $annotationKey as field key")
    }
    return FieldAnnotationKeyData(matcher[1]!!, matcher[2]!!)
}