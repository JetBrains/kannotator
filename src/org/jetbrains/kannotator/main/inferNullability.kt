package org.jetbrains.kannotator.main

import java.io.File
import org.jetbrains.kannotator.index.FileBasedClassSource
import org.jetbrains.kannotator.index.DeclarationIndexImpl
import org.jetbrains.kannotator.annotations.io.parseAnnotations
import java.io.FileReader
import org.jetbrains.kannotator.declarations.AnnotationsImpl
import org.jetbrains.kannotator.nullability.NullabilityAnnotation
import org.jetbrains.kannotator.index.AnnotationKeyIndex
import org.jetbrains.kannotator.declarations.Annotations
import org.jetbrains.kannotator.nullability.classNameToNullabilityAnnotation

fun inferNullabilityAnnotations(jarOrClassFiles: Collection<File>, existingAnnotationFiles: Collection<File>) {
    val source = FileBasedClassSource(jarOrClassFiles)
    val declarationIndex = DeclarationIndexImpl(source)

    val existingNullabilityAnnotations = loadNullabilityAnnotations(existingAnnotationFiles, declarationIndex)

    // build method graph

    // build hierarchy
}

fun loadNullabilityAnnotations(
        annotationFiles: Collection<File>,
        keyIndex: AnnotationKeyIndex): Annotations<NullabilityAnnotation>
{
    val nullabilityAnnotations = AnnotationsImpl<NullabilityAnnotation>()

    for (annotationFile in annotationFiles) {
        FileReader(annotationFile) use {
            parseAnnotations(it, {
                key, annotations ->
                val position = keyIndex.findPositionByAnnotationKeyString(key)
                if (position == null) {
                    error("Position not found for $key")
                }
                else {
                    for (data in annotations) {
                        val annotation = classNameToNullabilityAnnotation(data.annotationClassFqn)
                        if (annotation != null) {
                            nullabilityAnnotations[position] = annotation
                        }
                    }
                }
            }, {error(it)})
        }
    }

    return nullabilityAnnotations
}

fun error(message: String) {
    System.err.println(message)
}