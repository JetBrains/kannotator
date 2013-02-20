package org.jetbrains.kannotator.controlFlow.builder.analysis

import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.analysis.Frame
import org.objectweb.asm.Opcodes.*
import org.jetbrains.kannotator.index.DeclarationIndex
import org.objectweb.asm.tree.MethodInsnNode
import org.jetbrains.kannotator.annotationsInference.findMethodByMethodInsnNode
import org.jetbrains.kannotator.declarations.PositionsForMethod
import org.jetbrains.kannotator.annotationsInference.nullability.NullabilityAnnotation
import org.jetbrains.kannotator.declarations.Annotations
import org.jetbrains.kannotator.annotationsInference.findFieldByFieldInsnNode
import org.objectweb.asm.tree.FieldInsnNode
import org.jetbrains.kannotator.declarations.getFieldAnnotatedType
import java.util.Collections
import org.jetbrains.kannotator.annotationsInference.generateAssertsForCallArguments
import java.util.ArrayList
import java.util.HashMap
import org.jetbrains.kannotator.controlFlow.builder.*
import org.jetbrains.kannotator.declarations.Method
import org.objectweb.asm.tree.MethodNode
import org.jetbrains.kannotator.declarations.Field
import org.jetbrains.kannotator.index.FieldDependencyInfo
import org.jetbrains.kannotator.annotationsInference.nullability.*
import org.jetbrains.kannotator.controlFlow.builder.*
import org.jetbrains.kannotator.declarations.*
import java.util.HashSet
import org.jetbrains.kannotator.asm.util.isPrimitiveOrVoidType
import org.objectweb.asm.Type
import org.jetbrains.kannotator.annotationsInference.engine.*

object NULLABILITY_KEY

public enum class Nullability: Qualifier {
    // can't prove anything yet
    UNKNOWN

    // can prove that only NULL reaches the frame
    NULL

    // can prove that only non-NULL values reach the frame
    NOT_NULL

    // can prove that NULL as well as non-NULL values reach the frame
    NULLABLE

    // can prove that no values reach the frame
    EMPTY

    // special value used in "impose" to discard the proof and fall back to UNKNOWN
    DISCARD
}

public object NullabilitySet: QualifierSet<Nullability> {
    public override val id: Any = NULLABILITY_KEY
    public override val initial: Nullability = Nullability.UNKNOWN

    // Can assume that EMPTY and DISCARD do not appear as argument
    public override fun merge(q1: Nullability, q2: Nullability): Nullability {
        if (q1 == q2) {
            return q1
        }
        if (q1 == Nullability.NULLABLE || q2 == Nullability.NULLABLE) {
            return Nullability.NULLABLE
        }
        if (q1 == Nullability.NULL || q2 == Nullability.NULL) {
            return Nullability.NULLABLE
        }

        return Nullability.UNKNOWN
    }

    // Can assume that EMPTY does not appear as argument
    // and DISCARD can appear at most once
    public override fun impose(q1: Nullability, q2: Nullability): Nullability {
        if (q1 == q2) {
            return q1
        }
        if (q1 == Nullability.DISCARD) {
            return if (q2 != Nullability.NOT_NULL) Nullability.UNKNOWN else Nullability.NOT_NULL
        }
        if (q2 == Nullability.DISCARD) {
            return if (q1 != Nullability.NOT_NULL) Nullability.UNKNOWN else Nullability.NOT_NULL
        }
        if (q1 == Nullability.UNKNOWN) {
            return q2
        }
        if (q2 == Nullability.UNKNOWN) {
            return q1
        }
        if (q1 == Nullability.NULLABLE) {
            return q2
        }
        if (q2 == Nullability.NULLABLE) {
            return q1
        }
        return Nullability.EMPTY
    }

    public override fun contains(q: Qualifier): Boolean = q is Nullability
}

fun <Q: Qualifier> imposeNullabilityOnFrameValues(
        frame: Frame<QualifiedValueSet<Q>>, frameValues: QualifiedValueSet<Q>?, nullability: Nullability, updateOriginalValues: Boolean = true
): Frame<QualifiedValueSet<Q>>? {
    imposeQualifierOnFrameValues(frame, frameValues, nullability, NullabilitySet, updateOriginalValues)

    return if (frame.allValues { frameValue ->
        if (frameValue.values.empty)
            true
        else {
            val valueSet = frameValue.values as MutableSet<QualifiedValue<Q>>
            val it = valueSet.iterator()
            while (it.hasNext()) {
                val value = it.next()
                if (value.qualifier.extract<Nullability>(NullabilitySet) == Nullability.EMPTY) {
                    it.remove()
                }
            }
            !frameValue.values.empty
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

        return when (insnNode.getOpcode()) {
            GETFIELD, ARRAYLENGTH, ATHROW, MONITORENTER, MONITOREXIT -> {
                singletonOrEmptyResult(imposeNullabilityOnFrameValues(executedFrame.copy(), preFrame.getStackFromTop(0), Nullability.NULL))
            }
            AALOAD, BALOAD, IALOAD, CALOAD, SALOAD, FALOAD, LALOAD, DALOAD, PUTFIELD -> {
                singletonOrEmptyResult(imposeNullabilityOnFrameValues(executedFrame.copy(), preFrame.getStackFromTop(1), Nullability.NULL))
            }
            AASTORE, BASTORE, IASTORE, CASTORE, SASTORE, FASTORE, LASTORE, DASTORE -> {
                singletonOrEmptyResult(imposeNullabilityOnFrameValues(executedFrame.copy(), preFrame.getStackFromTop(2), Nullability.NULL))
            }
            INVOKEVIRTUAL, INVOKEINTERFACE, INVOKEDYNAMIC, INVOKESTATIC, INVOKESPECIAL -> {
                val results = ArrayList<ResultFrame<QualifiedValueSet<Q>>>()
                generateAssertsForCallArguments(insnNode, declarationIndex, annotations,
                        { indexFromTop ->
                            val newFrame = imposeNullabilityOnFrameValues(executedFrame.copy(), preFrame.getStackFromTop(indexFromTop), Nullability.NULL)
                            if (newFrame != null) {
                                results.add(pseudoErrorResult(newFrame))
                            }
                        },
                        true,
                        { paramAnnotation -> paramAnnotation == NullabilityAnnotation.NOT_NULL },
                        {})
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

        fun getPostFrameForSimpleDereferensing(stackIndexFromTop: Int): Frame<QualifiedValueSet<Q>>? {
            if (edgeKind == EdgeKind.DEFAULT)
                imposeNullabilityOnFrameValues(executedFrame.copy(), preFrame.getStackFromTop(stackIndexFromTop), Nullability.NOT_NULL)
            else defFrame
        }

        fun getPostFrameForMethodInvocation(): Frame<QualifiedValueSet<Q>>? {
            if (edgeKind == EdgeKind.DEFAULT) {
                var newFrame: Frame<QualifiedValueSet<Q>>? = executedFrame.copy()
                generateAssertsForCallArguments(insnNode, declarationIndex, annotations,
                        { indexFromTop ->
                            if (newFrame != null)
                                newFrame = imposeNullabilityOnFrameValues(newFrame!!, preFrame.getStackFromTop(indexFromTop), Nullability.NOT_NULL)
                        },
                        true,
                        { paramAnnotation -> paramAnnotation == NullabilityAnnotation.NOT_NULL },
                        { indexFromTop ->
                            if (newFrame != null)
                                newFrame = imposeNullabilityOnFrameValues(newFrame!!, preFrame.getStackFromTop(indexFromTop), Nullability.DISCARD)
                        })
                newFrame
            } else defFrame
        }

        fun getPostFrameForNullCheck(): Frame<QualifiedValueSet<Q>>? {
            val nullEdge = if (insnNode.getOpcode() == IFNULL) EdgeKind.TRUE else EdgeKind.FALSE
            val nonNullEdge = if (insnNode.getOpcode() == IFNULL) EdgeKind.FALSE else EdgeKind.TRUE
            when (edgeKind) {
                nonNullEdge -> imposeNullabilityOnFrameValues(executedFrame.copy(), preFrame.getStackFromTop(0), Nullability.NOT_NULL)
                nullEdge -> imposeNullabilityOnFrameValues(executedFrame.copy(), preFrame.getStackFromTop(0), Nullability.NULL)
                else -> defFrame
            }
        }

        fun getPostFrameForCondition(): Frame<QualifiedValueSet<Q>>? {
            return when (edgeKind) {
                EdgeKind.FALSE, EdgeKind.TRUE -> {
                    val newFrame: Frame<QualifiedValueSet<Q>>? = executedFrame.copy()
                    preFrame.forEachValue { frameValue ->
                        imposeNullabilityOnFrameValues(executedFrame.copy(), frameValue, Nullability.DISCARD)
                    }
                    newFrame
                }
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

            if (instanceOfFrame != null) {
                when (edgeKind) {
                    nonNullEdge -> imposeNullabilityOnFrameValues(executedFrame.copy(), instanceOfFrame.getStackFromTop(0), Nullability.NOT_NULL, false)
                    nullableEdge -> imposeNullabilityOnFrameValues(executedFrame.copy(), instanceOfFrame.getStackFromTop(0), Nullability.NULLABLE, false)
                    else -> defFrame
                }

            } else getPostFrameForCondition()
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
            IFLT, IFLE, IFGT, IFGE,
            IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE,
            IF_ACMPEQ, IF_ACMPNE -> {
                getPostFrameForCondition()
            }
            else -> defFrame
        }
    }
}

fun NullabilityAnnotation?.toQualifier() : Nullability = when (this) {
    NullabilityAnnotation.NOT_NULL -> Nullability.NOT_NULL
    NullabilityAnnotation.NULLABLE -> Nullability.NULLABLE
    null -> Nullability.UNKNOWN
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
                NEW, NEWARRAY, ANEWARRAY, MULTIANEWARRAY -> Nullability.NOT_NULL
                ACONST_NULL -> Nullability.NULL
                LDC -> Nullability.NOT_NULL
                INVOKEINTERFACE, INVOKESTATIC, INVOKESPECIAL, INVOKEVIRTUAL, INVOKESPECIAL -> {
                    val method = declarationIndex.findMethodByMethodInsnNode(createdAt as MethodInsnNode)
                    if (method != null) {
                        val positions = PositionsForMethod(method)
                        annotations[positions.forReturnType().position].toQualifier()
                    }
                    else Nullability.UNKNOWN
                }
                GETFIELD, GETSTATIC -> {
                    val field = declarationIndex.findFieldByFieldInsnNode(createdAt as FieldInsnNode)
                    if (field != null) {
                        annotations[getFieldAnnotatedType(field).position].toQualifier()
                    }
                    else Nullability.UNKNOWN
                }
                else -> Nullability.UNKNOWN
            }
        }

        if (baseValue.parameterIndex != null) {
            return annotations[positions.forParameter(baseValue.parameterIndex).position].toQualifier()
        }

        return when (baseValue._type) {
            NULL_TYPE -> Nullability.NULL
            PRIMITIVE_TYPE_SIZE_1, PRIMITIVE_TYPE_SIZE_2 -> Nullability.UNKNOWN
            else -> Nullability.NOT_NULL // this is either "this" or caught exception
        }
    }
}

fun <Q: Qualifier> QualifiedValueSet<Q>?.lub(): Nullability {
    return if (this != null)
        if (!this.values.empty) {
            this.values.fold(
                    Nullability.EMPTY,
                    { (q, v) -> lub(q, v.qualifier.extract<Nullability>(NullabilitySet)) }
            )
        } else {
            Nullability.UNKNOWN
        }
    else Nullability.UNKNOWN
}

fun lub(a: Nullability?, b: Nullability?): Nullability {
    val q1 = a ?: Nullability.UNKNOWN
    val q2 = b ?: Nullability.UNKNOWN

    if (q1 == Nullability.EMPTY) {
        return q2
    }
    if (q2 == Nullability.EMPTY) {
        return q1
    }

    return NullabilitySet.merge(q1, q2)
}

fun glb(a: Nullability?, b: Nullability?): Nullability {
    val q1 = a ?: Nullability.UNKNOWN
    val q2 = b ?: Nullability.UNKNOWN

    if (q1 == Nullability.NOT_NULL || q2 == Nullability.NOT_NULL)
        return Nullability.NOT_NULL
    if (q1 == Nullability.UNKNOWN && q2 == Nullability.UNKNOWN)
        return Nullability.UNKNOWN

    return Nullability.NULLABLE
}

fun Nullability.toAnnotation(): NullabilityAnnotation? {
    return when(this) {
        Nullability.NOT_NULL -> NullabilityAnnotation.NOT_NULL
        Nullability.NULL, Nullability.NULLABLE -> NullabilityAnnotation.NULLABLE
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
                (a, b) -> lub(a, b)
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

                        val frame = analysisResult.mergedFrames[methodNode.instructions.indexOf(insnNode)]
                        if (frame != null) {
                            val fieldValues = frame.getStackFromTop(0)
                            val nullabilityValueInfo = fieldValues.lub()

                            val autoCastedField : Field = field // Avoid KT-2746 bug
                            fieldInfoMap[autoCastedField] = lub(
                                    fieldInfoMap.getOrElse(autoCastedField, { Nullability.EMPTY }),
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
        var returnValueInfo: Nullability = Nullability.EMPTY

        for (resultFrame in analysisResult.returnedResults) {
            if (resultFrame.insnNode.getOpcode() == ARETURN) {
                val returnValues = resultFrame.frame.getStackFromTop(0)
                val nullability = returnValues.lub()
                returnValueInfo = lub(returnValueInfo, nullability)
            }
        }

        return if (returnValueInfo != Nullability.EMPTY)
            returnValueInfo
        else Nullability.UNKNOWN
    }

    fun buildMergedParameterMap(resultFrames: Collection<ResultFrame<QualifiedValueSet<Q>>>): Map<Int, Nullability> {
        if (resultFrames.empty) {
            return Collections.emptyMap()
        }

        val paramInfoMap = HashMap<Int, Nullability>()

        for (returnedResult in resultFrames) {
            val frame = returnedResult.frame
            val localParamInfoMap = HashMap<Int, Nullability>()

            for (i in 0..frame.getLocals() - 1) {
                val localVar = frame.getLocal(i)
                if (localVar != null) {
                    for (value in localVar.values) {
                        if (value.base.interesting) {
                            val index = value.base.parameterIndex!!
                            val currentInfo = (value.qualifier.extract<Nullability>(NullabilitySet)) ?: Nullability.UNKNOWN
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
        val returnInfoMap = buildMergedParameterMap(analysisResult.returnedResults)
        if (returnInfoMap.empty) {
            return returnInfoMap
        }

        val paramInfoMap = HashMap<Int, Nullability>()
        val errorInfoMap = buildMergedParameterMap(analysisResult.errorResults)
        for ((index, info) in returnInfoMap) {
            val pos = positions.forParameter(index).position
            val currentAnnotation = annotations[pos]

            if (info == Nullability.NOT_NULL && currentAnnotation != NullabilityAnnotation.NOT_NULL) {
                if (errorInfoMap.empty) {
                    paramInfoMap[index] = Nullability.NULLABLE
                } else when (errorInfoMap[index]) {
                    Nullability.UNKNOWN -> Nullability.UNKNOWN
                    Nullability.NOT_NULL -> paramInfoMap[index] = Nullability.NULLABLE
                    Nullability.NULL, Nullability.NULLABLE -> paramInfoMap[index] = Nullability.NOT_NULL
                    else -> assert(false, "Invalid nullability of parameter $index")
                }
            } else if (info == Nullability.UNKNOWN && currentAnnotation != null) {
                paramInfoMap[index] = currentAnnotation.toQualifier()
            } else { //info == NULL || NULLABLE
                paramInfoMap[index] = info
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
        inferredAnnotations.setIfNotNull(positions.forParameter(paramIndex).position, paramInfo.toAnnotation())
    }

    val fieldInfoMap = collectFieldNullability()
    val updatedFieldInfoProvider = {(m: Method) -> if (m == method) fieldInfoMap else methodFieldsNullabilityInfoProvider(m) }
    for (changedField in findFieldsWithChangedNullabilityInfo(methodFieldsNullabilityInfoProvider(method), fieldInfoMap)) {
        val fieldAnnotation = buildFieldNullabilityAnnotations(fieldDependencyInfoProvider(changedField), updatedFieldInfoProvider)
        if (fieldAnnotation != null) {
            inferredAnnotations[getFieldTypePosition(changedField)] = fieldAnnotation
        }
    }

    return AnnotationsBuildingResult(inferredAnnotations, fieldInfoMap)
}