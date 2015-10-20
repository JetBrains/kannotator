package org.jetbrains.kannotator.annotations.io

import annotations.SceneAnnotation
import annotations.el.*
import annotations.field.BasicAFT
import kotlinlib.mapValues
import org.jetbrains.kannotator.declarations.*

private fun AnnotationData.toSceneAnnotation(): SceneAnnotation {
    val annotationDef = AnnotationDef(this.annotationClassFqn)
    //setFieldTypes is mandatory - check assertions in checkRep() method of SceneAnnotation class
    //NB as kannotator only uses annotations with String attribute types, the following works. It should not matter anyway for serialization
    annotationDef.setFieldTypes(this.attributes.mapValues { k, v -> BasicAFT.forType(Class.forName("java.lang.String")) })
    return SceneAnnotation(annotationDef, this.attributes)
}

private fun convertMethod(
        annotationPosition: MethodTypePosition,
        classRecord: AClass,
        annotationDatas: Collection<AnnotationData>
) {
    val kMethod = annotationPosition.method
    val methodRecord = classRecord.methods.vivify(kMethod.id.methodName + kMethod.id.methodDesc)
    val relPosition = annotationPosition.relativePosition
    val sceneAnnotations = annotationDatas.map { it.toSceneAnnotation() }
    when (relPosition) {
        is RETURN_TYPE -> {
            methodRecord.tlAnnotationsHere.addAll(sceneAnnotations)
        }
        is ParameterPosition -> {
            val index = if (kMethod.isStatic()) relPosition.index else relPosition.index - 1
            val parameterElem = methodRecord.parameters.vivify(index)
            parameterElem.tlAnnotationsHere.addAll(sceneAnnotations)
        }
    }
}

private fun convertField (
        annotationPosition: FieldTypePosition,
        classRecord: AClass,
        annotationDatas: Collection<AnnotationData>
) {
    val sceneAnnotations = annotationDatas.map { it.toSceneAnnotation() }
    val fieldElem = classRecord.fields.vivify(annotationPosition.field.name)
    fieldElem.tlAnnotationsHere.addAll(sceneAnnotations)
}

public fun Map<AnnotationPosition, Collection<AnnotationData>>.toAScene(): AScene {
    val scene = AScene()
    for ((position, annData) in this) {
        scene.packages.vivify(position.getPackageName())

        val className = position.member.declaringClass.internal.replace('/', '.')
        val classRecord = scene.classes.vivify(className)

        when (position) {
            is MethodTypePosition ->
                convertMethod(position, classRecord, annData)
            is FieldTypePosition ->
                convertField(position, classRecord, annData)
            else -> {
                throw UnsupportedOperationException("AnnotationPosition subtypes other, than FieldTypePosition and MethodTypePosition are not implemented")
            }
        }
    }
    return scene
}

fun AElement.transformAnnotations(transform: (SceneAnnotation)->SceneAnnotation?) {
    val transformedList = tlAnnotationsHere.map (transform)
    if (thisType != null) thisType!!.transformAnnotations(transform)
    tlAnnotationsHere.clear()
    tlAnnotationsHere.addAll(transformedList.filterNotNull())
}

fun ATypeElement.transformAnnotations(transform: (SceneAnnotation)->SceneAnnotation?) {
    (this as AElement).transformAnnotations(transform)
    innerTypes.values
            .forEach { it.transformAnnotations(transform) }
}
fun AClass.transformAnnotations(transform: (SceneAnnotation)->SceneAnnotation?) {
    (this as AElement).transformAnnotations(transform)
    bounds
            .values.forEach { it.transformAnnotations(transform) }
    extendsImplements
            .values.forEach { it.transformAnnotations(transform) }
    methods
            .values.forEach { it.transformAnnotations(transform) }
    fields
            .values.forEach { it.transformAnnotations(transform) }
}

fun AMethod.transformAnnotations(transform: (SceneAnnotation)->SceneAnnotation?) {
    (this as AElement).transformAnnotations(transform)

    returnType.transformAnnotations(transform)
    receiver.transformAnnotations(transform)

    bounds
            .values.forEach { it.transformAnnotations(transform) }
    parameters
            .values.forEach { it.transformAnnotations(transform) }
    locals
            .values.forEach { it.transformAnnotations(transform) }
    typecasts
            .values.forEach { it.transformAnnotations(transform) }
    instanceofs
            .values.forEach { it.transformAnnotations(transform) }
    news
            .values.forEach { it.transformAnnotations(transform) }
}

public fun AScene.transformAnnotations(transform: (SceneAnnotation)->SceneAnnotation?) {
    for((packagename, packagerecord) in packages)
        packagerecord.transformAnnotations (transform)
    for((classname, classrecord) in classes)
        classrecord.transformAnnotations(transform)
}