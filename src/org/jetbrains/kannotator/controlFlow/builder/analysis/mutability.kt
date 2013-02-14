package org.jetbrains.kannotator.controlFlow.builder.analysis

import org.objectweb.asm.tree.analysis.Frame
import org.jetbrains.kannotator.controlFlow.builder.PossibleTypedValues
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.MethodInsnNode
import org.jetbrains.kannotator.annotationsInference.mutability.*
import org.jetbrains.kannotator.declarations.PositionsForMethod
import org.jetbrains.kannotator.declarations.Annotations
import org.jetbrains.kannotator.annotationsInference.findFieldByFieldInsnNode
import org.objectweb.asm.tree.FieldInsnNode
import org.jetbrains.kannotator.declarations.getFieldAnnotatedType
import java.util.Collections
import org.jetbrains.kannotator.annotationsInference.generateAssertsForCallArguments
import java.util.ArrayList
import java.util.HashMap
import org.jetbrains.kannotator.index.DeclarationIndex
import org.objectweb.asm.tree.MethodNode
import org.jetbrains.kannotator.declarations.ClassName
import org.jetbrains.kannotator.controlFlow.builder.*
import org.jetbrains.kannotator.declarations.*
import java.util.HashSet
import org.objectweb.asm.Type

object MUTABILITY_KEY

public enum class Mutability: Qualifier {
    READ_ONLY
    MUTABLE
}

object MutabilitySet: QualifierSet<Mutability> {
    public override val id: Any = MUTABILITY_KEY
    public override val initial: Mutability = Mutability.READ_ONLY

    public override fun merge(q1: Mutability, q2: Mutability): Mutability {
        return if (q1 == q2) q1 else Mutability.MUTABLE
    }

    public override fun impose(q1: Mutability, q2: Mutability): Mutability = merge(q1, q2)

    public override fun contains(q: Qualifier): Boolean = q is Mutability
}

fun <Q: Qualifier> imposeMutabilityOnFrameValues(
        frame: Frame<QualifiedValueSet<Q>>, frameValues: QualifiedValueSet<Q>?, mutability: Mutability, analyzer: Analyzer<QualifiedValueSet<Q>>
): Frame<QualifiedValueSet<Q>> {
    imposeQualifierOnFrameValues(frame, frameValues, mutability, MutabilitySet, true)

    if (frameValues != null) {
        for (value in frameValues.values) {
            val createdAtInsn = value.base.createdAt
            if (createdAtInsn is MethodInsnNode && createdAtInsn.isMutabilityPropagatingInvocation()) {
                val createdAtFrame = analyzer.getInstructionFrame(createdAtInsn)!!
                imposeMutabilityOnFrameValues(frame, createdAtInsn.getReceiver(createdAtFrame), Mutability.MUTABLE, analyzer)
            }
        }
    }

    return frame
}

class MutabilityFrameTransformer<Q: Qualifier>(
        val annotations: Annotations<MutabilityAnnotation>,
        val declarationIndex: DeclarationIndex
): BasicFrameTransformer<Q>() {
    public override fun getPostFrame(
            insnNode: AbstractInsnNode,
            edgeIndex: Int,
            preFrame: Frame<QualifiedValueSet<Q>>,
            executedFrame: Frame<QualifiedValueSet<Q>>,
            analyzer: Analyzer<QualifiedValueSet<Q>>
    ): Frame<QualifiedValueSet<Q>>? {
        val defFrame = super<BasicFrameTransformer>.getPostFrame(insnNode, edgeIndex, preFrame, executedFrame, analyzer)

        val opcode = insnNode.getOpcode()
        return when (opcode) {
            INVOKEVIRTUAL, INVOKEINTERFACE, INVOKEDYNAMIC, INVOKESTATIC, INVOKESPECIAL -> {
                val methodInsnNode = insnNode as MethodInsnNode
                val postFrame = executedFrame.copy()
                if (opcode == INVOKEINTERFACE && methodInsnNode.isMutatingInvocation()) {
                    imposeMutabilityOnFrameValues(postFrame, methodInsnNode.getReceiver(preFrame), Mutability.MUTABLE, analyzer)
                }

                generateAssertsForCallArguments(insnNode, declarationIndex, annotations,
                        { indexFromTop -> imposeMutabilityOnFrameValues(postFrame, preFrame.getStackFromTop(indexFromTop), Mutability.MUTABLE, analyzer)},
                        false,
                        { paramAnnotation -> paramAnnotation == MutabilityAnnotation.MUTABLE },
                        {}
                )

                postFrame
            }

            else -> defFrame
        }
    }
}

object MutabilityQualifierEvaluator: QualifierEvaluator<Mutability> {
    override fun evaluateQualifier(baseValue: TypedValue): Mutability = Mutability.READ_ONLY
}

fun <Q: Qualifier> buildMutabilityAnnotations(
        method: Method,
        analysisResult: AnalysisResult<QualifiedValueSet<Q>>
) : Annotations<MutabilityAnnotation> {
    val positions = PositionsForMethod(method)

    val affectedValues = HashSet<QualifiedValue<Q>>()
    for (resultFrame in analysisResult.returnedResults) {
        resultFrame.frame.forEachValue { frameValue ->
            frameValue.values.forEach { v ->
                if (v.base.interesting && v.qualifier.extract<Mutability>(MutabilitySet) == Mutability.MUTABLE) {
                    affectedValues.add(v)
                }
            }
        }
    }

    val result = AnnotationsImpl<MutabilityAnnotation>()
    for (value in affectedValues) {
        val pos = positions.forParameter(value.base.parameterIndex!!).position
        result.setIfNotNull(pos, MutabilityAnnotation.MUTABLE)
    }

    return result
}