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

private val METHOD_ANNOTATION_KEY_PATTERN = Pattern.compile("""([\w\.]+\$?) (.*?)\s?([\w<>$]+)\(""")
private val FIELD_ANNOTATION_KEY_PATTERN = Pattern.compile("""(\w+(\.\w+)*\$?)\s+([\w\$]+)""")

fun tryParseMethodAnnotationKey(annotationKey: String): MethodAnnotationKeyData? {
    val matcher = METHOD_ANNOTATION_KEY_PATTERN.matcher(annotationKey)
    if (!matcher.find()) {
        return null
    }
    return MethodAnnotationKeyData(matcher[1]!!, matcher[2]!!, matcher[3]!!)
}

fun parseMethodAnnotationKey(annotationKey: String): MethodAnnotationKeyData {
    val data = tryParseMethodAnnotationKey(annotationKey)
    if (data != null)
        return data
    throw IllegalArgumentException("Can't parse annotation key $annotationKey as method key")
}

fun tryParseFieldAnnotationKey(annotationKey: String): FieldAnnotationKeyData? {
    val matcher = FIELD_ANNOTATION_KEY_PATTERN.matcher(annotationKey)
    if (!matcher.find()) {
        return null
    }
    return FieldAnnotationKeyData(matcher[1]!!, matcher[3]!!)
}

fun parseFieldAnnotationKey(annotationKey: String): FieldAnnotationKeyData {
    val data = tryParseFieldAnnotationKey(annotationKey)
    if (data != null)
        return data
    throw IllegalArgumentException("Can't parse annotation key $annotationKey as field key")
}