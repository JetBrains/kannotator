package org.jetbrains.kannotator.annotationsInference.nullability

import org.objectweb.asm.Opcodes.*
import kotlin.test.assertTrue
import org.jetbrains.kannotator.annotationsInference.AnnotationsInference
import org.jetbrains.kannotator.annotationsInference.AnnotationsManager
import org.jetbrains.kannotator.annotationsInference.Assert
import org.jetbrains.kannotator.annotationsInference.nullability.NullabilityValueInfo.*
import org.jetbrains.kannotator.asm.util.getOpcode
import org.jetbrains.kannotator.controlFlow.ControlFlowGraph
import org.jetbrains.kannotator.controlFlow.Instruction
import org.jetbrains.kannotator.controlFlow.Value
import org.jetbrains.kannotator.controlFlowBuilder.STATE_BEFORE
import org.jetbrains.kannotator.declarations.Annotations
import org.jetbrains.kannotator.declarations.AnnotationsImpl
import org.jetbrains.kannotator.declarations.PositionsWithinMember
import org.jetbrains.kannotator.declarations.TypePosition
import org.jetbrains.kannotator.index.DeclarationIndex

class NullabilityAnnotationsInference(
        graph: ControlFlowGraph,
        override protected val annotations: Annotations<NullabilityAnnotation>,
        positions: PositionsWithinMember,
        declarationIndex: DeclarationIndex
) : AnnotationsInference<NullabilityAnnotation, NullabilityValueInfo>(graph, annotations, positions, declarationIndex,
        NullabilityAnnotationsManager(annotations, declarationIndex, positions)) {

    //todo make property without backing field (after KT-2892)
    private val nullabilityAnnotationManager : NullabilityAnnotationsManager = annotationsManager as NullabilityAnnotationsManager
    private val framesManager = FramesNullabilityManager(nullabilityAnnotationManager, annotations, declarationIndex)

    override fun computeValueInfos(instruction: Instruction) : ValueNullabilityMap =
            framesManager.computeNullabilityInfosForInstruction(instruction)

    override fun isAnnotationNecessary(
            assert: Assert,
            valueInfos: Map<Value, NullabilityValueInfo>
    ): Boolean {
        if (!assert.value.interesting) return false

        val valueInfo = valueInfos[assert.value]
        return valueInfo == NULLABLE || valueInfo == UNKNOWN
    }

    protected override fun generateAsserts(instruction: Instruction): Collection<Assert> =
        generateNullabilityAsserts(instruction, annotations, declarationIndex)

    override fun postProcess() {
        framesManager.clear()
        traverseInstructions { instruction ->
            checkReturnInstruction(instruction, computeValueInfos(instruction))
        }
    }

    private fun generateNullabilityAsserts(
            instruction: Instruction,
            annotations: Annotations<NullabilityAnnotation>,
            declarationIndex: DeclarationIndex
    ) : Set<Assert> {
        val state = instruction[STATE_BEFORE]!!
        val result = hashSet<Assert>()

        fun addAssertForStackValue(indexFromTop: Int) {
            val valueSet = state.stack[indexFromTop]
            for (value in valueSet) {
                result.add(Assert(value))
            }
        }

        when (instruction.getOpcode()) {
            INVOKEVIRTUAL, INVOKEINTERFACE, INVOKEDYNAMIC, INVOKESTATIC -> {
                generateAssertsForCallArguments(instruction, { indexFromTop -> addAssertForStackValue(indexFromTop) },
                        true, { paramAnnotation -> paramAnnotation == NullabilityAnnotation.NOT_NULL })
            }
            GETFIELD, ARRAYLENGTH, ATHROW,
            MONITORENTER, MONITOREXIT -> {
                addAssertForStackValue(0)
            }
            AALOAD, BALOAD, IALOAD, CALOAD, SALOAD, FALOAD, LALOAD, DALOAD,
            PUTFIELD -> {
                addAssertForStackValue(1)
            }
            AASTORE, BASTORE, IASTORE, CASTORE, SASTORE, FASTORE, LASTORE, DASTORE -> {
                addAssertForStackValue(2)
            }
            else -> {}
        }
        return result
    }

    private fun checkReturnInstruction(
            instruction: Instruction,
            nullabilityInfos: ValueNullabilityMap
    ) {
        val state = instruction[STATE_BEFORE]!!

        when (instruction.getOpcode()) {
            ARETURN -> {
                val valueSet = state.stack[0]
                val nullabilityValueInfo = valueSet.map { it -> nullabilityInfos[it] }.merge()
                nullabilityAnnotationManager.addReturnValueInfo(nullabilityValueInfo)

                nullabilityAnnotationManager.addValueNullabilityMapOnReturn(nullabilityInfos)
            }
            RETURN, IRETURN, LRETURN, DRETURN, FRETURN -> {
                nullabilityAnnotationManager.addValueNullabilityMapOnReturn(nullabilityInfos)
            }
            else -> Unit.VALUE
        }
    }
}

private class NullabilityAnnotationsManager(
        val annotations: Annotations<NullabilityAnnotation>,
        val declarationIndex: DeclarationIndex,
        val positions: PositionsWithinMember
) : AnnotationsManager<NullabilityAnnotation>() {

    val parameterAnnotations = hashMap<Value, NullabilityAnnotation>()
    val valueNullabilityMapsOnReturn = arrayList<ValueNullabilityMap>()
    var returnValueInfo : NullabilityValueInfo? = null

    fun addParameterAnnotation(value: Value, annotation: NullabilityAnnotation) {
        parameterAnnotations[value] = annotation
    }

    fun addValueNullabilityMapOnReturn(map: ValueNullabilityMap) {
        valueNullabilityMapsOnReturn.add(map)
    }

    fun addReturnValueInfo(valueInfo: NullabilityValueInfo) {
        val current = returnValueInfo
        returnValueInfo = valueInfo.mergeWithNullable(current)
    }

    override fun addAssert(assert: Assert) {
        addParameterAnnotation(assert.value, NullabilityAnnotation.NOT_NULL)
    }

    fun getParameterAnnotation(value: Value) : NullabilityAnnotation? {
        assertTrue(value.interesting)
        val inferredNow = parameterAnnotations[value]
        val alreadyKnown = annotations[value.getParameterPosition()]
        return if (inferredNow != null) inferredNow else alreadyKnown
    }

    private fun Value.getParameterPosition() = positions.forParameter(this.parameterIndex!!).position

    override fun toAnnotations(): Annotations<NullabilityAnnotation> {
        val annotations = AnnotationsImpl<NullabilityAnnotation>()
        fun setAnnotation(position: TypePosition, annotation: NullabilityAnnotation?) {
            if (annotation != null) {
                annotations[position] = annotation
            }
        }
        setAnnotation(positions.forReturnType().position, returnValueInfo?.toAnnotation())

        val mapOnReturn = mergeValueNullabilityMaps(this, annotations, declarationIndex, valueNullabilityMapsOnReturn)
        for ((value, valueInfo) in mapOnReturn) {
            if (value.interesting) {
                setAnnotation(value.getParameterPosition(), valueInfo.toAnnotation())
            }
        }
        for ((value, annotation) in parameterAnnotations) {
            setAnnotation(value.getParameterPosition(), annotation)
        }
        return annotations
    }
}
