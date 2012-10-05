package org.jetbrains.kannotator.annotationsInference

import org.objectweb.asm.Opcodes.*
import org.jetbrains.kannotator.controlFlow.ControlFlowGraph
import org.jetbrains.kannotator.controlFlow.Instruction
import org.jetbrains.kannotator.controlFlow.Value
import org.jetbrains.kannotator.controlFlowBuilder.AsmInstructionMetadata
import org.jetbrains.kannotator.controlFlowBuilder.STATE_BEFORE
import org.jetbrains.kannotator.declarations.Annotations
import org.jetbrains.kannotator.declarations.AnnotationsImpl
import org.jetbrains.kannotator.declarations.Positions
import org.jetbrains.kannotator.declarations.TypePosition
import org.jetbrains.kannotator.nullability.NullabilityAnnotation
import org.jetbrains.kannotator.nullability.NullabilityValueInfo
import org.jetbrains.kannotator.nullability.NullabilityValueInfo.*
import org.jetbrains.kannotator.nullability.merge
import org.jetbrains.kannotator.nullability.toAnnotation
import org.jetbrains.kannotator.declarations.Method
import org.jetbrains.kannotator.index.DeclarationIndex
import org.jetbrains.kannotator.nullability.Nullability
import org.jetbrains.kannotator.asm.util.getOpcode
import org.jetbrains.kannotator.nullability.mergeWithNullable
import kotlin.test.assertTrue
import org.jetbrains.kannotator.declarations.getArgumentCount

class NullabilityAnnotationsInference(
        graph: ControlFlowGraph,
        override protected val annotations: Annotations<NullabilityAnnotation>,
        positions: Positions,
        declarationIndex: DeclarationIndex
) : AnnotationsInference<Nullability>(graph, annotations, positions, declarationIndex,
        NullabilityAnnotationsManager(annotations, declarationIndex, positions)) {

    //todo make property without backing field (after KT-2892)
    private val nullabilityAnnotationManager : NullabilityAnnotationsManager = annotationsManager as NullabilityAnnotationsManager
    private val framesManager = FramesNullabilityManager(nullabilityAnnotationManager, annotations, declarationIndex)

    override fun computeValueInfos(instruction: Instruction) : ValueNullabilityMap =
            framesManager.computeNullabilityInfosForInstruction(instruction)

    override fun isAnnotationNecessary(
            assert: Assert<Nullability>,
            valueInfos: Map<Value, ValueInfo<Nullability>>
    ): Boolean {
        if (!assert.value.interesting) return false

        val valueInfo = valueInfos[assert.value]
        return valueInfo == NULLABLE || valueInfo == UNKNOWN
    }

    protected override fun generateAsserts(instruction: Instruction): Collection<Assert<Nullability>> =
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
    ) : Set<Assert<Nullability>> {
        val state = instruction[STATE_BEFORE]!!
        val result = hashSet<Assert<Nullability>>()

        fun addAssertForStackValue(indexFromTop: Int) {
            val valueSet = state.stack[indexFromTop]
            for (value in valueSet) {
                result.add(Assert(value))
            }
        }

        when (instruction.getOpcode()) {
            INVOKEVIRTUAL, INVOKEINTERFACE, INVOKEDYNAMIC, INVOKESTATIC -> {
                val methodId = getMethodIdByInstruction(instruction)
                val hasThis = instruction.getOpcode() != INVOKESTATIC
                val nonThisParametersCount = methodId!!.getArgumentCount() // excluding this
                if (hasThis) {
                    addAssertForStackValue(nonThisParametersCount)
                }
                if (instruction.getOpcode() != INVOKEDYNAMIC) {
                    val method = declarationIndex.findMethodByInstruction(instruction)
                    if (method != null) {
                        val positions = Positions(method)
                        val parameterIndices = if (hasThis) 1..nonThisParametersCount else 0..nonThisParametersCount - 1
                        for (paramIndex in parameterIndices) {
                            val paramAnnotation = annotations[positions.forParameter(paramIndex).position]
                            if (paramAnnotation == NullabilityAnnotation.NOT_NULL) {
                                addAssertForStackValue(nonThisParametersCount - paramIndex)
                            }
                        }
                    }
                }
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
        val positions: Positions
) : AnnotationsManager<Nullability>() {

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

    override fun addAssert(assert: Assert<Nullability>) {
        addParameterAnnotation(assert.value, NullabilityAnnotation.NOT_NULL)
    }

    fun getParameterAnnotation(value: Value) : NullabilityAnnotation? {
        assertTrue(value.interesting)
        val inferredNow = parameterAnnotations[value]
        val alreadyKnown = annotations[value.getParameterPosition()]
        return if (inferredNow != null) inferredNow else alreadyKnown
    }

    private fun Value.getParameterPosition() = positions.forParameter(this.parameterIndex!!).position

    override fun toAnnotations(): Annotations<Annotation<Nullability>> {
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
