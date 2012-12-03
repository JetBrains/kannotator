package org.jetbrains.kannotator.annotationsInference.nullability

import java.util.HashMap
import java.util.HashSet
import org.jetbrains.kannotator.annotationsInference.forInterestingValue
import org.jetbrains.kannotator.annotationsInference.traverseInstructions
import org.jetbrains.kannotator.asm.util.getAsmInstructionNode
import org.jetbrains.kannotator.asm.util.getOpcode
import org.jetbrains.kannotator.asm.util.isPrimitiveOrVoidType
import org.jetbrains.kannotator.controlFlow.ControlFlowGraph
import org.jetbrains.kannotator.controlFlow.Instruction
import org.jetbrains.kannotator.controlFlow.ControlFlowEdge
import org.jetbrains.kannotator.controlFlow.builder.STATE_BEFORE
import org.jetbrains.kannotator.declarations.*
import org.jetbrains.kannotator.index.DeclarationIndex
import org.jetbrains.kannotator.index.FieldDependencyInfo
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.FieldInsnNode
import java.util.LinkedHashSet
import org.jetbrains.kannotator.controlFlow.InstructionMetadata
import org.jetbrains.kannotator.util.DataKey
import java.util.ArrayList
import java.util.Collections
import kotlinlib.removeLast
import java.util.ArrayDeque
import org.jetbrains.kannotator.controlFlow.MethodOutcome

class AnnotationsBuildingResult(
        val inferredAnnotations: Annotations<NullabilityAnnotation>,
        val writtenFieldValueInfos: Map<Field, NullabilityValueInfo>)

fun buildMethodNullabilityAnnotations(
        method: Method,
        cfGraph: ControlFlowGraph,
        fieldDependencyInfoProvider: (Field) -> FieldDependencyInfo,
        declarationIndex: DeclarationIndex,
        annotations: Annotations<NullabilityAnnotation>,
        methodFieldsNullabilityInfoProvider: (Method) -> Map<Field, NullabilityValueInfo>?
): AnnotationsBuildingResult {
    val positions = PositionsForMethod(method)
    val framesManager = FramesNullabilityManager(positions, annotations, declarationIndex)
    val valueNullabilityMapsOnReturn = arrayList<ValueNullabilityMap>()
    var returnValueInfo : NullabilityValueInfo? = null

    fun collectValueInfoForReturnInstruction(
            instruction: Instruction,
            nullabilityInfos: ValueNullabilityMap,
            overridingMap: ValueNullabilityMap
    ) {
        if (instruction.getOpcode() == ARETURN) {
            val state = instruction[STATE_BEFORE]!!
            val returnValues = state.stack[0]
            val nullabilityValueInfo = returnValues.map{ it ->
                if (overridingMap.containsKey(it)) overridingMap[it] else nullabilityInfos[it]
            }.merge()
            returnValueInfo = nullabilityValueInfo.mergeWithNullable(returnValueInfo)
        }
        valueNullabilityMapsOnReturn.add(nullabilityInfos)
    }

    val fieldNullabilityInfoMap = HashMap<Field, NullabilityValueInfo>()
    fun collectValueInfoForFieldInitInstruction(instruction: Instruction, nullabilityInfos: ValueNullabilityMap) {
        if (instruction.getOpcode() == PUTSTATIC || instruction.getOpcode() == PUTFIELD) {
            val fieldNode = instruction.getAsmInstructionNode() as FieldInsnNode
            val field = declarationIndex.findField(ClassName.fromInternalName(fieldNode.owner), fieldNode.name)

            if (field != null && shouldCollectNullabilityInfo(field)) {
                assert(field.name == fieldNode.name)

                val state = instruction[STATE_BEFORE]!!
                val fieldValues = state.stack[0]
                val nullabilityValueInfo = fieldValues.map { it -> nullabilityInfos[it] }.merge()

                val autoCastedField : Field = field // Avoid KT-2746 bug
                fieldNullabilityInfoMap[autoCastedField] = nullabilityValueInfo merge
                        fieldNullabilityInfoMap.getOrElse(autoCastedField, { NullabilityValueInfo.CONFLICT })
            }
        }
    }

    fun createAnnotations(overridingMap: ValueNullabilityMap): Annotations<NullabilityAnnotation> {
        val result = AnnotationsImpl<NullabilityAnnotation>()
        result.setIfNotNull(positions.forReturnType().position, returnValueInfo?.toAnnotation())

        val (mapOnReturn) = mergeValueNullabilityMaps(positions, result, declarationIndex, valueNullabilityMapsOnReturn)
        for ((value, valueInfo) in mapOnReturn) {
            if (!value.interesting) {
                continue
            }

            val valuePosition = positions.forInterestingValue(value)

            val nullability = if (overridingMap.containsKey(value)) {
                overridingMap.getStored(value)
            } else {
                if (mapOnReturn.spoiledValues.contains(value))
                    NullabilityValueInfo.NULLABLE
                else
                    valueInfo
            }

            result.setIfNotNull(valuePosition, nullability.toAnnotation())
        }

        val updatedFieldInfoProvider = {(m: Method) -> if (m == method) fieldNullabilityInfoMap else methodFieldsNullabilityInfoProvider(m) }

        for (changedField in findFieldsWithChangedNullabilityInfo(methodFieldsNullabilityInfoProvider(method), fieldNullabilityInfoMap)) {
            val fieldAnnotation = buildFieldNullabilityAnnotations(fieldDependencyInfoProvider(changedField), updatedFieldInfoProvider)
            if (fieldAnnotation != null) {
                result[getFieldTypePosition(changedField)] = fieldAnnotation
            }
        }

        return result
    }

    val overridingNullabilityMap = ValueNullabilityMap(positions, annotations, declarationIndex)
    val inferenceContext = InferenceContext(overridingNullabilityMap, cfGraph.instructionOutcomes)

    cfGraph.traverseInstructions {instruction ->
        val valueNullabilityMap =
                framesManager.computeNullabilityInfosForInstruction(instruction, inferenceContext)

        if (instruction.getOpcode() in RETURN_OPCODES) {
            collectValueInfoForReturnInstruction(instruction, valueNullabilityMap, overridingNullabilityMap)
        }

        collectValueInfoForFieldInitInstruction(instruction, valueNullabilityMap)
    }

    return AnnotationsBuildingResult(
            createAnnotations(overridingNullabilityMap),
            fieldNullabilityInfoMap
    )
}

data class InferenceContext(
        val overridingNullabilityMap: ValueNullabilityMap,
        val instructionOutcomes: Map<Instruction, MethodOutcome>
)

fun findFieldsWithChangedNullabilityInfo(previous: Map<Field, NullabilityValueInfo>?,
                                         new: Map<Field, NullabilityValueInfo>) : Collection<Field> {
    if (previous == null) {
        return new.keySet()
    }

    val changedInFields = HashSet<Field>()
    assert(previous.keySet() == new.keySet())

    for (key in previous.keySet()) {
        if (previous[key] != new[key]) {
            changedInFields.add(key)
        }
    }

    return changedInFields
}

private val RETURN_OPCODES = hashSet(ARETURN, RETURN, IRETURN, LRETURN, DRETURN, FRETURN)

private fun FieldDependencyInfo.areAllWritersProcessed(methodFieldsNullabilityInfoProvider: (Method) -> Map<Field, NullabilityValueInfo>?) : Boolean =
        this.writers.all { methodFieldsNullabilityInfoProvider(it) != null }

private fun buildFieldNullabilityAnnotations(
        fieldInfo: FieldDependencyInfo,
        methodToFieldsNullabilityProvider: (Method) -> Map<Field, NullabilityValueInfo>?): NullabilityAnnotation? {
    val fromValueAnnotation = inferAnnotationsFromFieldValue(fieldInfo.field)

    if (fromValueAnnotation != null) {
        return fromValueAnnotation
    }

    if (!fieldInfo.field.getType().isPrimitiveOrVoidType()) {
        if (fieldInfo.areAllWritersProcessed(methodToFieldsNullabilityProvider)) {
            val fieldNullabilityInfos = fieldInfo.writers.map { writer -> methodToFieldsNullabilityProvider(writer)!!.get(fieldInfo.field)!! }
            return fieldNullabilityInfos.merge().toAnnotation()
        }
    }

    return null
}

fun shouldCollectNullabilityInfo(field: Field): Boolean {
    return !field.getType().isPrimitiveOrVoidType() && field.isFinal()
}

fun inferAnnotationsFromFieldValue(field: Field) : NullabilityAnnotation? {
    if (field.isFinal() && field.value != null && !field.getType().isPrimitiveOrVoidType()) {
        // A very simple case when final field has initial value
        return NullabilityAnnotation.NOT_NULL
    }

    return null
}
