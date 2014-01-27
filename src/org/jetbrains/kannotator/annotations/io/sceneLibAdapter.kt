package org.jetbrains.kannotator.annotations.io

import kotlinlib.*
import org.jetbrains.kannotator.declarations.*
import annotations.el.AScene
import annotations.el.AClass
import annotations.el.AnnotationDef
import annotations.SceneAnnotation
import annotations.field.BasicAFT
import annotations.el.AElement
import annotations.el.ATypeElement
import annotations.el.AMethod

private fun AnnotationData.toSceneAnnotation(): SceneAnnotation {
    val annotationDef = AnnotationDef(this.annotationClassFqn)
    //setFieldTypes is mandatory - check assertions in checkRep() method of SceneAnnotation class
    //NB as kannotator only uses annotations with String attribute types, the following works. It should not matter anyway for serialization
    annotationDef.setFieldTypes(this.attributes.mapValues {(k, v)-> BasicAFT.forType(Class.forName("java.lang.String")) })
    return SceneAnnotation(annotationDef, this.attributes)
}

private fun convertMethod(
        annotationPosition: MethodTypePosition,
        classRecord: AClass,
        annotationDatas: Collection<AnnotationData>
) {
    val kMethod = annotationPosition.method
    val methodRecord = classRecord.methods.vivify(kMethod.id.methodName + kMethod.id.methodDesc)

    for((idx, param) in kMethod.parameterNames.withIndices()){
        methodRecord.parameters.vivify(idx)
                .thisType!!.tlAnnotationsHere.addAll(annotationDatas.map { it.toSceneAnnotation() })
    }
}

private fun convertField (
        annotationPosition: FieldTypePosition,
        classRecord: AClass,
        annotationDatas: Collection<AnnotationData>
) {
    classRecord.fields.vivify(annotationPosition.field.name)
            .thisType!!.tlAnnotationsHere.addAll(annotationDatas.map { it.toSceneAnnotation() })
}

public fun Map<AnnotationPosition, Collection<AnnotationData>>.toAScene(): AScene {
    val scene = AScene()
    for ((position, annData) in this) {
        scene.packages.vivify(position.getPackageName())

        val className = position.member.declaringClass.name
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
    (this:AElement).transformAnnotations(transform)
    innerTypes.values()
            .forEach { it.transformAnnotations(transform) }
}
fun AClass.transformAnnotations(transform: (SceneAnnotation)->SceneAnnotation?) {
    (this : AElement).transformAnnotations(transform)
    bounds
            .values().forEach { it.transformAnnotations(transform) }
    extendsImplements
            .values().forEach { it.transformAnnotations(transform) }
    methods
            .values().forEach { it.transformAnnotations(transform) }
    fields
            .values().forEach { it.transformAnnotations(transform) }
}

fun AMethod.transformAnnotations(transform: (SceneAnnotation)->SceneAnnotation?) {
    (this : AElement).transformAnnotations(transform)

    returnType.transformAnnotations(transform)
    receiver.transformAnnotations(transform)

    bounds
            .values().forEach { it.transformAnnotations(transform) }
    parameters
            .values().forEach { it.transformAnnotations(transform) }
    locals
            .values().forEach { it.transformAnnotations(transform) }
    typecasts
            .values().forEach { it.transformAnnotations(transform) }
    instanceofs
            .values().forEach { it.transformAnnotations(transform) }
    news
            .values().forEach { it.transformAnnotations(transform) }
}

public fun AScene.transformAnnotations(transform: (SceneAnnotation)->SceneAnnotation?) {
    for((packagename, packagerecord) in packages)
        packagerecord.transformAnnotations (transform)
    for((classname, classrecord) in classes)
        classrecord.transformAnnotations(transform)
}