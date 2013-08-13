package org.jetbrains.kannotator.annotations.io

import kotlinlib.*
import org.jetbrains.kannotator.declarations.*
import annotations.el.AScene
import annotations.el.AClass
import annotations.el.AnnotationDef
import annotations.SceneAnnotation
import annotations.field.BasicAFT

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
    val methodRecord = classRecord.methods.vivify(kMethod.id.methodName+kMethod.id.methodDesc)

    for((idx, param) in kMethod.parameterNames.withIndices()){
        val paramRecord = methodRecord.parameters.vivify(idx)
        val annotationsSet = paramRecord.tlAnnotationsHere as MutableSet<annotations.SceneAnnotation>

        annotationsSet.addAll(annotationDatas.map { it.toSceneAnnotation() })

    }
}

private fun convertField (
        annotationPosition: FieldTypePosition,
        classRecord: AClass,
        annotationDatas: Collection<AnnotationData>
) {
    val kField = annotationPosition.field
    val fieldRecord = classRecord.fields.vivify(kField.name)
    val annotationsSet = fieldRecord.tlAnnotationsHere as MutableSet<annotations.SceneAnnotation>
    annotationsSet.addAll(annotationDatas.map { it.toSceneAnnotation() })
}

public fun Map<AnnotationPosition, Collection<AnnotationData>>.toAScene(): AScene
{
    val scene = AScene()
    for ((position, annData) in this) {
        scene.packages.vivify(position.getPackageName())

        val classCanonicalName = position.member.declaringClass.canonicalName
        val classRecord = scene.classes.vivify(classCanonicalName)

        when (position){
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