package org.jetbrains.kannotator.annotations.io

import java.util.regex.Pattern
import kotlinlib.*

data class AnnotationKeyData(
        val canonicalClassName: String,
        val returnType: String,
        val methodName: String
)

private val ANNOTATION_KEY_PATTERN = Pattern.compile("""([\w\.]+) (.*?)\s?([\w<>$]+)\(""")!!

fun parseAnnotationKey(annotationKey: String): AnnotationKeyData {
    val matcher = ANNOTATION_KEY_PATTERN.matcher(annotationKey)
    if (!matcher.find()) {
        throw IllegalArgumentException("Can't parse annotation key $annotationKey")
    }
    return AnnotationKeyData(matcher[1]!!, matcher[2]!!, matcher[3]!!)
}