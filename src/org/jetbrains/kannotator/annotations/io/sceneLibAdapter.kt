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
import annotations.el.InnerTypeLocation
import annotations.ext.TypePathEntry

import org.objectweb.asm.Type

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
    val relPosition = annotationPosition.relativePosition
    val sceneAnnotations = annotationDatas.map { it.toSceneAnnotation() }
    when (relPosition) {
        is RETURN_TYPE -> {
            val returnTypeElem = methodRecord.returnType
            val returnType = kMethod.getReturnType()
            elementToAnnotate(returnTypeElem, returnType).tlAnnotationsHere.addAll(sceneAnnotations)
        }
        is ParameterPosition -> {
            val index = if (kMethod.isStatic()) relPosition.index else  relPosition.index - 1
            val parameterTypeElem = methodRecord.parameters.vivify(index).thisType!!
            val parameterType = kMethod.getArgumentTypes()[index]
            elementToAnnotate(parameterTypeElem, parameterType).tlAnnotationsHere.addAll(sceneAnnotations)
        }
    }
}

private fun convertField (
        annotationPosition: FieldTypePosition,
        classRecord: AClass,
        annotationDatas: Collection<AnnotationData>
) {
    val sceneAnnotations = annotationDatas.map { it.toSceneAnnotation() }
    val fieldTypeElem = classRecord.fields.vivify(annotationPosition.field.name).thisType!!
    val fieldType = annotationPosition.field.getType()
    elementToAnnotate(fieldTypeElem, fieldType).tlAnnotationsHere.addAll(sceneAnnotations)
}

// takes into account inner path for arrays
// see http://types.cs.washington.edu/annotation-file-utilities/annotation-file-format.html (section Compound type annotations)
private fun elementToAnnotate(element: ATypeElement, tp: Type) : ATypeElement {
    return when (tp.getSort()) {
        Type.ARRAY -> {
            val loc = InnerTypeLocation((1..tp.getDimensions()).map { TypePathEntry.fromBinary(0, 0)!! })
            element.innerTypes.vivify(loc)
        }
        else ->
            element
    }
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