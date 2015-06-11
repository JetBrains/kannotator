package org.jetbrains.kannotator.controlFlow.builder.analysis

import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.analysis.Frame
import org.objectweb.asm.Opcodes.*
import org.jetbrains.kannotator.index.DeclarationIndex
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.FieldInsnNode
import java.util.Collections
import java.util.ArrayList
import java.util.HashMap
import org.objectweb.asm.tree.MethodNode
import org.jetbrains.kannotator.index.FieldDependencyInfo
import org.jetbrains.kannotator.declarations.*
import java.util.HashSet
import org.jetbrains.kannotator.asm.util.isPrimitiveOrVoidType
import org.objectweb.asm.Type
import org.jetbrains.kannotator.annotationsInference.engine.*
import org.jetbrains.kannotator.controlFlow.builder.analysis.Nullability.*
import org.jetbrains.kannotator.annotationsInference.nullability.NullabilityAnnotation
import org.jetbrains.kannotator.runtime.annotations.AnalysisType

// TODO: http://youtrack.jetbrains.com/issue/KT-4275
import org.jetbrains.kannotator.controlFlow.builder.analysis.Nullability.NULL as NULL_

class NullabilityKey : AnalysisType {
    override fun toString() = "nullability"
}

val NULLABILITY_KEY = NullabilityKey()

public enum class Nullability: Qualifier {
    // can't prove anything yet
    UNKNOWN,

    // can prove that only NULL reaches the frame
    NULL,

    // can prove that only non-NULL values reach the frame
    NOT_NULL,

    // can prove that NULL as well as non-NULL values reach the frame
    NULLABLE,

    // can prove that no values reach the frame
    EMPTY
}

public object NullabilitySet: QualifierSet<Nullability> {
    public override val id: AnalysisType = NULLABILITY_KEY
    public override val initial: Nullability = UNKNOWN

    // Can assume that EMPTY and DISCARD do not appear as argument
    public override fun merge(q1: Nullability, q2: Nullability): Nullability {
        if (q1 == q2) {
            return q1
        }
        if (q1 == NULLABLE || q2 == NULLABLE) {
            return NULLABLE
        }
        if (q1 == NULL_ || q2 == NULL_) {
            return NULLABLE
        }

        return UNKNOWN
    }

    public override fun contains(q: Qualifier): Boolean = q is Nullability
}

val imposeNull = {
    q: Nullability -> if (q != NOT_NULL) NULL_ else EMPTY
}

val imposeNotNull = {
    q: Nullability -> if (q != NULL_) NOT_NULL else EMPTY
}

val imposeNullable = {
    q: Nullability -> if (q == NOT_NULL || q == NULL_) q else NULLABLE
}

val imposeUndecidable = {
    q: Nullability -> if (q != NOT_NULL) UNKNOWN else NOT_NULL
}

fun <Q: Qualifier> imposeNullabilityOnFrameValues(
        frame: Frame<QualifiedValueSet<Q>>, frameValues: QualifiedValueSet<Q>?, updateOriginalValues: Boolean,
        transform: (Nullability) -> Nullability
): Frame<QualifiedValueSet<Q>>? {
    updateQualifiers(frame, frameValues, NullabilitySet, updateOriginalValues, transform)

    return if (frame.allValues { frameValue ->
        if (frameValue.values.isEmpty())
            true
        else {
            val valueSet = frameValue.values as MutableSet<QualifiedValue<Q>>
            val it = valueSet.iterator()
            while (it.hasNext()) {
                val value = it.next()
                if (value.qualifier.extract<Nullability>(NullabilitySet) == EMPTY) {
                    it.remove()
                }
            }
            !frameValue.values.isEmpty()
        }

    }) frame else null
}

class NullabilityFrameTransformer<Q: Qualifier>(
        val annotations: Annotations<NullabilityAnnotation>,
        val declarationIndex: DeclarationIndex
): BasicFrameTransformer<Q>() {
    public override fun getPseudoResults(
            insnNode: AbstractInsnNode,
            preFrame: Frame<QualifiedValueSet<Q>>,
            executedFrame: Frame<QualifiedValueSet<Q>>,
            analyzer: Analyzer<QualifiedValueSet<Q>>
    ): Collection<ResultFrame<QualifiedValueSet<Q>>> {
        val defFrame = super<BasicFrameTransformer>.getPseudoResults(insnNode, preFrame, executedFrame, analyzer)
        val insnIndex = analyzer.getInstructionIndex(insnNode)

        return when (insnNode.getOpcode()) {
            GETFIELD, ARRAYLENGTH, ATHROW, MONITORENTER, MONITOREXIT -> {
                singletonOrEmptyResult(imposeNullabilityOnFrameValues(executedFrame.copy(), preFrame.getStackFromTop(0), true, imposeNull), insnIndex)
            }
            AALOAD, BALOAD, IALOAD, CALOAD, SALOAD, FALOAD, LALOAD, DALOAD, PUTFIELD -> {
                singletonOrEmptyResult(imposeNullabilityOnFrameValues(executedFrame.copy(), preFrame.getStackFromTop(1), true, imposeNull), insnIndex)
            }
            AASTORE, BASTORE, IASTORE, CASTORE, SASTORE, FASTORE, LASTORE, DASTORE -> {
                singletonOrEmptyResult(imposeNullabilityOnFrameValues(executedFrame.copy(), preFrame.getStackFromTop(2), true, imposeNull), insnIndex)
            }
            INVOKEVIRTUAL, INVOKEINTERFACE, INVOKEDYNAMIC, INVOKESTATIC, INVOKESPECIAL -> {
                val results = ArrayList<ResultFrame<QualifiedValueSet<Q>>>()
                generateAssertsForCallArguments(insnNode, declarationIndex, annotations,
                        { indexFromTop ->
                            val newFrame = imposeNullabilityOnFrameValues(executedFrame.copy(), preFrame.getStackFromTop(indexFromTop), true, imposeNull)
                            if (newFrame != null) {
                                results.add(pseudoErrorResult(newFrame, insnIndex))
                            }
                        },
                        true,
                        { paramAnnotation -> paramAnnotation == NullabilityAnnotation.NOT_NULL },
                        { indexFromTop ->
                            val newFrame = imposeNullabilityOnFrameValues(executedFrame.copy(), preFrame.getStackFromTop(indexFromTop), true) {
                                q -> if (q != NOT_NULL) q else EMPTY
                            }
                            if (newFrame != null) {
                                results.add(pseudoErrorResult(newFrame, insnIndex))
                            }
                        }
                )
                results
            }
            else -> defFrame
        }
    }

    public override fun getPostFrame(
            insnNode: AbstractInsnNode,
            edgeKind: EdgeKind,
            preFrame: Frame<QualifiedValueSet<Q>>,
            executedFrame: Frame<QualifiedValueSet<Q>>,
            analyzer: Analyzer<QualifiedValueSet<Q>>
    ): Frame<QualifiedValueSet<Q>>? {
        val defFrame = super<BasicFrameTransformer>.getPostFrame(insnNode, edgeKind, preFrame, executedFrame, analyzer)

        fun getPostFrameForSimpleDereferensing(stackIndexFromTop: Int): Frame<QualifiedValueSet<Q>>? =
            if (edgeKind == EdgeKind.DEFAULT)
                imposeNullabilityOnFrameValues(executedFrame.copy(), preFrame.getStackFromTop(stackIndexFromTop), true, imposeNotNull)
            else defFrame

        fun getPostFrameForMethodInvocation(): Frame<QualifiedValueSet<Q>>? =
            if (edgeKind == EdgeKind.DEFAULT) {
                var newFrame: Frame<QualifiedValueSet<Q>>? = executedFrame.copy()
                generateAssertsForCallArguments(insnNode, declarationIndex, annotations,
                        { indexFromTop ->
                            if (newFrame != null)
                                newFrame = imposeNullabilityOnFrameValues(newFrame!!, preFrame.getStackFromTop(indexFromTop), true, imposeNotNull)
                        },
                        true,
                        { paramAnnotation -> paramAnnotation == NullabilityAnnotation.NOT_NULL },
                        { indexFromTop ->
                            if (newFrame != null)
                                newFrame = imposeNullabilityOnFrameValues(newFrame!!, preFrame.getStackFromTop(indexFromTop), true, imposeUndecidable)
                        })
                newFrame
            } else defFrame

        fun getPostFrameForNullCheck(): Frame<QualifiedValueSet<Q>>? {
            val nullEdge = if (insnNode.getOpcode() == IFNULL) EdgeKind.TRUE else EdgeKind.FALSE
            val nonNullEdge = if (insnNode.getOpcode() == IFNULL) EdgeKind.FALSE else EdgeKind.TRUE
            return when (edgeKind) {
                nonNullEdge -> imposeNullabilityOnFrameValues(executedFrame.copy(), preFrame.getStackFromTop(0), true, imposeNotNull)
                nullEdge -> imposeNullabilityOnFrameValues(executedFrame.copy(), preFrame.getStackFromTop(0), true, imposeNull)
                else -> defFrame
            }
        }

        fun getPostFrameForEqualityCheck(): Frame<QualifiedValueSet<Q>>? {
            val conditions = preFrame.getStackFromTop(0)
            val instanceOfFrame = if (conditions != null) {
                var origFrame: Frame<QualifiedValueSet<Q>>? = null
                conditions.values.any { condition ->
                    val origInsnNode = condition.base.createdAt
                    val found = (origInsnNode != null && origInsnNode.getOpcode() == INSTANCEOF)
                    if (found)
                        origFrame = analyzer.getInstructionFrame(origInsnNode!!)
                    found
                }
                origFrame
            } else null
            val nonNullEdge = if (insnNode.getOpcode() == IFEQ) EdgeKind.FALSE else EdgeKind.TRUE
            val nullableEdge = if (insnNode.getOpcode() == IFEQ) EdgeKind.TRUE else EdgeKind.FALSE

            return if (instanceOfFrame != null) {
                when (edgeKind) {
                    nonNullEdge -> imposeNullabilityOnFrameValues(executedFrame.copy(), instanceOfFrame.getStackFromTop(0), false, imposeNotNull)
                    nullableEdge -> imposeNullabilityOnFrameValues(executedFrame.copy(), instanceOfFrame.getStackFromTop(0), false, imposeNullable)
                    else -> defFrame
                }

            } else defFrame
        }

        return when (insnNode.getOpcode()) {
            GETFIELD, ARRAYLENGTH, ATHROW, MONITORENTER, MONITOREXIT -> {
                getPostFrameForSimpleDereferensing(0)
            }
            AALOAD, BALOAD, IALOAD, CALOAD, SALOAD, FALOAD, LALOAD, DALOAD, PUTFIELD -> {
                getPostFrameForSimpleDereferensing(1)
            }
            AASTORE, BASTORE, IASTORE, CASTORE, SASTORE, FASTORE, LASTORE, DASTORE -> {
                getPostFrameForSimpleDereferensing(2)
            }
            INVOKEVIRTUAL, INVOKEINTERFACE, INVOKEDYNAMIC, INVOKESTATIC, INVOKESPECIAL -> {
                getPostFrameForMethodInvocation()
            }
            IFNULL, IFNONNULL -> {
                getPostFrameForNullCheck()
            }
            IFEQ, IFNE -> {
                getPostFrameForEqualityCheck()
            }
            else -> defFrame
        }
    }
}

fun NullabilityAnnotation?.toQualifier() : Nullability = when (this) {
    NullabilityAnnotation.NOT_NULL -> NOT_NULL
    NullabilityAnnotation.NULLABLE -> NULLABLE
    null -> UNKNOWN
}

class NullabilityQualifierEvaluator(
        val positions: PositionsForMethod,
        val annotations: Annotations<NullabilityAnnotation>,
        val declarationIndex: DeclarationIndex
): QualifierEvaluator<Nullability> {
    override fun evaluateQualifier(baseValue: TypedValue): Nullability {
        val createdAt = baseValue.createdAt
        if (createdAt != null) {
            return when (createdAt.getOpcode()) {
                NEW, NEWARRAY, ANEWARRAY, MULTIANEWARRAY -> NOT_NULL
                ACONST_NULL -> NULL_
                LDC -> NOT_NULL
                INVOKEINTERFACE, INVOKESTATIC, INVOKESPECIAL, INVOKEVIRTUAL, INVOKESPECIAL -> {
                    val method = declarationIndex.findMethodByMethodInsnNode(createdAt as MethodInsnNode)
                    if (method != null) {
                        val positions = PositionsForMethod(method)
                        annotations[positions.forReturnType().position].toQualifier()
                    }
                    else UNKNOWN
                }
                GETFIELD, GETSTATIC -> {
                    val field = declarationIndex.findFieldByFieldInsnNode(createdAt as FieldInsnNode)
                    if (field != null) {
                        annotations[getFieldAnnotatedType(field).position].toQualifier()
                    }
                    else UNKNOWN
                }
                else -> UNKNOWN
            }
        }

        if (baseValue.parameterIndex != null) {
            return annotations[positions.forParameter(baseValue.parameterIndex).position].toQualifier()
        }

        return when (baseValue._type) {
            NULL_TYPE -> NULL_
            PRIMITIVE_TYPE_SIZE_1, PRIMITIVE_TYPE_SIZE_2 -> UNKNOWN
            else -> NOT_NULL // this is either "this" or caught exception
        }
    }
}

fun <Q: Qualifier> QualifiedValueSet<Q>?.lub(): Nullability {
    return if (this != null)
        if (!this.values.isEmpty()) {
            this.values.fold(
                    EMPTY,
                    { q, v -> lub(q, v.qualifier.extract<Nullability>(NullabilitySet)) }
            )
        } else {
            UNKNOWN
        }
    else UNKNOWN
}

fun lub(a: Nullability?, b: Nullability?): Nullability {
    val q1 = a ?: UNKNOWN
    val q2 = b ?: UNKNOWN

    if (q1 == EMPTY) {
        return q2
    }
    if (q2 == EMPTY) {
        return q1
    }

    return NullabilitySet.merge(q1, q2)
}

fun glb(a: Nullability?, b: Nullability?): Nullability {
    val q1 = a ?: UNKNOWN
    val q2 = b ?: UNKNOWN

    if (q1 == NOT_NULL || q2 == NOT_NULL)
        return NOT_NULL
    if (q1 == UNKNOWN && q2 == UNKNOWN)
        return UNKNOWN

    return NULLABLE
}

fun Nullability.toAnnotation(): NullabilityAnnotation? {
    return when(this) {
        NOT_NULL -> NullabilityAnnotation.NOT_NULL
        NULL_, NULLABLE -> NullabilityAnnotation.NULLABLE
        else -> null
    }
}

class AnnotationsBuildingResult(
        val inferredAnnotations: Annotations<NullabilityAnnotation>,
        val writtenFieldValueInfos: Map<Field, Nullability>)

fun findFieldsWithChangedNullabilityInfo(previous: Map<Field, Nullability>?,
                                         new: Map<Field, Nullability>) : Collection<Field> {
    if (previous == null) {
        return new.keySet()
    }

    val changedInFields = HashSet<Field>()

    for (key in previous.keySet()) {
        if (previous[key] != new[key]) {
            changedInFields.add(key)
        }
    }

    return changedInFields
}

fun shouldCollectNullabilityInfo(field: Field): Boolean {
    return !field.getType().isPrimitiveOrVoidType() && field.isFinal()
}

fun inferNullabilityFromFieldValue(field: Field) : NullabilityAnnotation? {
    if (field.isFinal() && field.value != null && !field.getType().isPrimitiveOrVoidType()) {
        // A very simple case when final field has initial value
        return NullabilityAnnotation.NOT_NULL
    }

    return null
}

private fun FieldDependencyInfo.areAllWritersProcessed(methodFieldsNullabilityInfoProvider: (Method) -> Map<Field, Nullability>?) : Boolean =
        this.writers.all { methodFieldsNullabilityInfoProvider(it) != null }

private fun buildFieldNullabilityAnnotations(
        fieldInfo: FieldDependencyInfo,
        methodToFieldsNullabilityProvider: (Method) -> Map<Field, Nullability>?): NullabilityAnnotation? {
    val fromValueAnnotation = inferNullabilityFromFieldValue(fieldInfo.field)

    if (fromValueAnnotation != null) {
        return fromValueAnnotation
    }

    if (!fieldInfo.field.getType().isPrimitiveOrVoidType()) {
        if (fieldInfo.areAllWritersProcessed(methodToFieldsNullabilityProvider)) {
            val fieldNullabilityInfos = fieldInfo.writers.map { writer -> methodToFieldsNullabilityProvider(writer)!!.get(fieldInfo.field)!! }
            return fieldNullabilityInfos.reduce {
                a, b -> lub(a, b)
            }.toAnnotation()
        }
    }

    return null
}

fun <Q: Qualifier> buildMethodNullabilityAnnotations(
        method: Method,
        methodNode: MethodNode,
        analysisResult: AnalysisResult<QualifiedValueSet<Q>>,
        fieldDependencyInfoProvider: (Field) -> FieldDependencyInfo,
        declarationIndex: DeclarationIndex,
        annotations: Annotations<NullabilityAnnotation>,
        methodFieldsNullabilityInfoProvider: (Method) -> Map<Field, Nullability>?
): AnnotationsBuildingResult {
    val positions = PositionsForMethod(method)

    fun collectFieldNullability(): Map<Field, Nullability> {
        val fieldInfoMap = HashMap<Field, Nullability>()

        for (insnNode in methodNode.instructions) {
            when (insnNode.getOpcode()) {
                PUTSTATIC, PUTFIELD -> {
                    val fieldNode = insnNode as FieldInsnNode
                    val field = declarationIndex.findField(ClassName.fromInternalName(fieldNode.owner), fieldNode.name)

                    if (field != null && shouldCollectNullabilityInfo(field)) {
                        assert(field.name == fieldNode.name)

                        val frame = analysisResult.mergedFrames[insnNode]
                        if (frame != null) {
                            val fieldValues = frame.getStackFromTop(0)
                            val nullabilityValueInfo = fieldValues.lub()

                            val autoCastedField : Field = field // Avoid KT-2746 bug
                            fieldInfoMap[autoCastedField] = lub(
                                    fieldInfoMap.getOrElse(autoCastedField, { EMPTY }),
                                    nullabilityValueInfo
                            )
                        }
                    }
                }
                else -> {}
            }
        }

        return fieldInfoMap
    }

    fun collectReturnValueNullability(): Nullability {
        var returnValueInfo: Nullability = EMPTY

        for (returnInsn in analysisResult.returnInstructions) {
            val resultFrame = analysisResult.mergedFrames[returnInsn]!!
            if (returnInsn.getOpcode() == ARETURN) {
                val returnValues = resultFrame.getStackFromTop(0)
                val nullability = returnValues.lub()
                returnValueInfo = lub(returnValueInfo, nullability)
            }
        }

        return if (returnValueInfo != EMPTY)
            returnValueInfo
        else UNKNOWN
    }

    fun buildNullLostParamSet(insnSet: Set<AbstractInsnNode>): Set<Int> {
        if (insnSet.isEmpty()) {
            return Collections.emptySet()
        }

        val set = HashSet<Int>()

        for (insn in insnSet) {
            val frame = analysisResult.mergedFrames[insn]!! as InferenceFrame<QualifiedValueSet<Q>>

            val lostValue = frame.getLostValue()
            if (lostValue != null) {
                for (value in lostValue.values) {
                    if (value.base.interesting) {
                        val index = value.base.parameterIndex!!
                        val currentInfo = (value.qualifier.extract<Nullability>(NullabilitySet)) ?: UNKNOWN
                        if (currentInfo == NULL_ || currentInfo == NULLABLE) {
                            set.add(index)
                        }
                    }
                }
            }
        }

        return set
    }

    fun buildMergedParameterMap(insnSet: Set<AbstractInsnNode>): Map<Int, Nullability> {
        if (insnSet.isEmpty()) {
            return Collections.emptyMap()
        }

        val paramInfoMap = HashMap<Int, Nullability>()

        for (insn in insnSet) {
            val frame = analysisResult.mergedFrames[insn]!!
            val localParamInfoMap = HashMap<Int, Nullability>()

            for (i in 0..frame.getLocals() - 1) {
                val localVar = frame.getLocal(i)
                if (localVar != null) {
                    for (value in localVar.values) {
                        if (value.base.interesting) {
                            val index = value.base.parameterIndex!!
                            val currentInfo = (value.qualifier.extract<Nullability>(NullabilitySet)) ?: UNKNOWN
                            if (!localParamInfoMap.containsKey(index)) {
                                localParamInfoMap[index] = currentInfo
                            }
                        }
                    }
                }
            }

            for ((index, currentInfo) in localParamInfoMap) {
                val prevInfo = paramInfoMap[index]
                paramInfoMap[index] =
                if (prevInfo != null)
                    NullabilitySet.merge(prevInfo, currentInfo)
                else currentInfo
            }
        }

        return paramInfoMap
    }

    fun collectParamNullability(): Map<Int, Nullability> {
        val returnInfoMap = buildMergedParameterMap(analysisResult.returnInstructions)
        if (returnInfoMap.isEmpty()) {
            return returnInfoMap
        }

        val errorInfoMap = buildMergedParameterMap(analysisResult.errorInstructions)
        val nullLostParams = buildNullLostParamSet(analysisResult.returnInstructions)

        val paramInfoMap = HashMap<Int, Nullability>()
        for ((index, info) in returnInfoMap) {
            val errorInfo = errorInfoMap[index]

            if (info != UNKNOWN && (errorInfo == NOT_NULL || errorInfo == null)) {
                paramInfoMap[index] = NULLABLE
            } else if ((errorInfo == NULL_ || errorInfo == NULLABLE) && info == NOT_NULL && !nullLostParams.contains(index)) {
                paramInfoMap[index] = NOT_NULL
            } else {
                paramInfoMap[index] = UNKNOWN
            }
        }

        return paramInfoMap
    }

    val inferredAnnotations = AnnotationsImpl<NullabilityAnnotation>()

    if (!Type.getReturnType(methodNode.desc).isPrimitiveOrVoidType()) {
        val returnValueInfo = collectReturnValueNullability()
        inferredAnnotations.setIfNotNull(positions.forReturnType().position, returnValueInfo.toAnnotation())
    }

    val paramInfoMap = collectParamNullability()
    for ((paramIndex, paramInfo) in paramInfoMap) {
        val pos = positions.forParameter(paramIndex).position
        val prevAnnotation = annotations[pos]
        val currAnnotation = paramInfo.toAnnotation()
        val newAnnotation =
                if (currAnnotation == null) prevAnnotation
                else if (prevAnnotation == null) currAnnotation
                else if (currAnnotation == prevAnnotation) prevAnnotation
                else if (prevAnnotation == NullabilityAnnotation.NOT_NULL && currAnnotation == NullabilityAnnotation.NULLABLE) NullabilityAnnotation.NOT_NULL
                else NullabilityAnnotation.NOT_NULL
        inferredAnnotations.setIfNotNull(pos, newAnnotation)
    }

    val fieldInfoMap = collectFieldNullability()
    val updatedFieldInfoProvider = { m: Method -> if (m == method) fieldInfoMap else methodFieldsNullabilityInfoProvider(m) }
    for (changedField in findFieldsWithChangedNullabilityInfo(methodFieldsNullabilityInfoProvider(method), fieldInfoMap)) {
        val fieldAnnotation = buildFieldNullabilityAnnotations(fieldDependencyInfoProvider(changedField), updatedFieldInfoProvider)
        if (fieldAnnotation != null) {
            inferredAnnotations[getFieldTypePosition(changedField)] = fieldAnnotation
        }
    }

    return AnnotationsBuildingResult(inferredAnnotations, fieldInfoMap)
}