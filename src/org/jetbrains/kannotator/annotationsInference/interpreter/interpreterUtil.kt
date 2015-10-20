package org.jetbrains.kannotator.controlFlow.builder.analysis

import org.jetbrains.kannotator.declarations.ClassName
import org.jetbrains.kannotator.asm.util.*
import org.jetbrains.kannotator.annotationsInference.engine.*

import org.objectweb.asm.*
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type.*
import org.objectweb.asm.tree.*
import org.objectweb.asm.tree.analysis.Value
import org.objectweb.asm.tree.analysis.Frame

import java.util.*
import com.gs.collections.impl.map.strategy.mutable.UnifiedMapWithHashingStrategy
import com.gs.collections.api.block.HashingStrategy
import org.jetbrains.kannotator.declarations.Method
import org.jetbrains.kannotator.index.DeclarationIndex
import org.jetbrains.kannotator.declarations.Annotations
import org.jetbrains.kannotator.declarations.PositionsForMethod
import org.jetbrains.kannotator.declarations.Field
import org.jetbrains.kannotator.runtime.annotations.AnalysisType

interface Annotation

public fun <A: Annotation> generateAssertsForCallArguments(
        instructionNode: AbstractInsnNode?,
        declarationIndex: DeclarationIndex,
        annotations: Annotations<A>,
        addAssertForStackValue: (Int) -> Unit,
        needGenerateAssertForThis: Boolean,
        needGenerateAssertForArgument: (A?) -> Boolean,
        addAssertForExternalStackValue: (Int) -> Unit
) {
    if (instructionNode !is MethodInsnNode) throw IllegalArgumentException("Not a method instruction: $instructionNode")
    val hasThis = instructionNode.opcode != INVOKESTATIC
    val thisSlots = if (hasThis) 1 else 0
    val parametersCount = instructionNode.getArgumentCount() + thisSlots
    val argTypes = Type.getArgumentTypes(instructionNode.desc)

    fun addAssertForArgumentOnStack(index: Int, external: Boolean) {
        if (index < thisSlots || !argTypes[index - thisSlots].isPrimitiveOrVoidType()) {
            val indexFromTop = parametersCount - index - 1
            if (external)
                addAssertForExternalStackValue(indexFromTop)
            else
                addAssertForStackValue(indexFromTop)
        }
    }

    if (hasThis && needGenerateAssertForThis) {
        addAssertForArgumentOnStack(0, false)
    }
    if (instructionNode.opcode != INVOKEDYNAMIC) {
        val method = declarationIndex.findMethodByMethodInsnNode(instructionNode)
        if (method != null) {
            val positions = PositionsForMethod(method)
            for (paramIndex in thisSlots..parametersCount - 1) {
                val paramAnnotation = annotations[positions.forParameter(paramIndex).position]
                if (paramAnnotation != null) {
                    if (needGenerateAssertForArgument(paramAnnotation)) {
                        addAssertForArgumentOnStack(paramIndex, false)
                    }
                } else {
                    addAssertForArgumentOnStack(paramIndex, true)
                }
            }
            return
        }
    }

    for (paramIndex in thisSlots..parametersCount - 1) {
        addAssertForArgumentOnStack(paramIndex, true)
    }
}

fun DeclarationIndex.findMethodByMethodInsnNode(methodInsnNode: MethodInsnNode): Method? {
    return this.findMethod(ClassName.fromInternalName(methodInsnNode.owner!!), methodInsnNode.name!!, methodInsnNode.desc)
}

fun DeclarationIndex.findFieldByFieldInsnNode(fieldInsnNode: FieldInsnNode): Field? {
    return this.findField(ClassName.fromInternalName(fieldInsnNode.owner), fieldInsnNode.name)
}

public fun <V: CopyableValue<V>> Frame<V>.copy(): Frame<V> {
    val frameCopy = InferenceFrame<V>(this)

    for (i in 0..locals - 1) {
        val v = getLocal(i)
        if (v != null) {
            frameCopy.setLocal(i, v.copy())
        }
    }

    frameCopy.clearStack()
    for (i in 0..stackSize - 1) {
        val v = getStack(i)
        if (v != null) {
            frameCopy.push(v.copy())
        }
    }

    frameCopy.lostValue = (this as InferenceFrame<V>).lostValue?.copy()

    return frameCopy
}

fun <V: Value> Frame<V>.forEachValue(body: (value: V) -> Unit) {
    for (i in 0..this.locals - 1) {
        val v = this.getLocal(i)
        if (v != null) {
            body(v)
        }
    }
    for (i in 0..this.stackSize - 1) {
        val v = this.getStack(i)
        if (v != null) {
            body(v)
        }
    }
}

fun <V: Value> Frame<V>.allValues(body: (value: V) -> Boolean): Boolean {
    for (i in 0..this.locals - 1) {
        val v = this.getLocal(i)
        if (v != null && !body(v)) {
            return false
        }
    }

    for (i in 0..this.stackSize - 1) {
        val v = this.getStack(i)
        if (v != null && !body(v)) {
            return false
        }
    }

    return true
}

fun <Q: Qualifier> Qualifier.extract(qualifierSet: QualifierSet<Q>): Q? {
    if (qualifierSet.contains(this)) {
        return this as Q
    }
    if (this is MultiQualifier<*>) {
        return this.qualifiers.getRaw(qualifierSet.id) as Q?
    }
    throw IllegalArgumentException("Can't extract qualifier")
}

fun <Q: Qualifier, I: Qualifier> QualifiedValue<Q>.copy(qualifierSet: QualifierSet<I>, qualifier: I): QualifiedValue<Q> {
    val thisQualifier = this.qualifier
    if (qualifierSet.contains(thisQualifier)) {
        return this.copy(qualifier as Q)
    }
    if (thisQualifier is MultiQualifier<*>) {
        val thisQualifierCopy = (thisQualifier as MultiQualifier<AnalysisType>).copy(qualifierSet.id, qualifier)
        return this.copy(thisQualifierCopy as Q)
    }
    throw IllegalArgumentException("Can't extract qualifier")
}

class QualifiedValueHashingStrategy<Q: Qualifier, I: Qualifier>(
        val qualifierSet: QualifierSet<I>
) : HashingStrategy<QualifiedValue<Q>> {
    public override fun equals(object1: QualifiedValue<Q>?, object2: QualifiedValue<Q>?): Boolean {
        if (object1 === object2) return true
        if (object1 == null || object2 == null) return false

        if (object1.base.id != object2.base.id) return false
        return (object1.qualifier.extract<I>(qualifierSet) == object2.qualifier.extract<I>(qualifierSet))
    }

    public override fun computeHashCode(_object: QualifiedValue<Q>?): Int {
        if (_object == null) return 0
        return 19*_object.base.id + (_object.qualifier.extract<I>(qualifierSet)?.hashCode() ?: 0)
    }
}

fun <Q: Qualifier, I: Qualifier> updateQualifiers(
        frame: Frame<QualifiedValueSet<Q>>,
        frameValues: QualifiedValueSet<Q>?,
        qualifierSet: QualifierSet<I>,
        updateOriginalValues: Boolean,
        transform: (I) -> I
): Frame<QualifiedValueSet<Q>> {
    if (frameValues != null) {
        val map = UnifiedMapWithHashingStrategy<QualifiedValue<Q>, QualifiedValue<Q>>(QualifiedValueHashingStrategy(qualifierSet))

        for (stackValue in frameValues.values) {
            val q1 = stackValue.qualifier.extract<I>(qualifierSet) ?: qualifierSet.initial
            val q2 = transform(q1)
            if (q2 != q1) {
                map.put(stackValue, stackValue.copy(qualifierSet, q2))
            }
        }

        if (updateOriginalValues) {
            val valueSet = frameValues.values as MutableSet<QualifiedValue<Q>>
            for ((origValue, newValue) in map.entries) {
                valueSet.remove(origValue)
                valueSet.add(newValue)
            }
        }

        val replacedValues = HashSet<QualifiedValue<Q>>()
        frame.forEachValue { valueSet ->
            replacedValues.clear()
            for (value in valueSet.values) {
                if (map.containsKey(value)) {
                    replacedValues.add(value)
                }
            }
            for (value in replacedValues) {
                (valueSet.values as MutableSet<QualifiedValue<Q>>).remove(value)
                valueSet.values.add(map[value]!!)
            }
        }
    }

    return frame
}

open class BasicFrameTransformer<Q: Qualifier>: DefaultFrameTransformer<QualifiedValueSet<Q>>() {
    public override fun getPostFrame(
            insnNode: AbstractInsnNode,
            edgeKind: EdgeKind,
            preFrame: Frame<QualifiedValueSet<Q>>,
            executedFrame: Frame<QualifiedValueSet<Q>>,
            analyzer: Analyzer<QualifiedValueSet<Q>>): Frame<QualifiedValueSet<Q>>? {
        if (insnNode.opcode == ASTORE && edgeKind == EdgeKind.DEFAULT) {
            val postFrame = executedFrame.copy()
            val varIndex = (insnNode as VarInsnNode).`var`
            val rhs = preFrame.getStackFromTop(0)
            val lhs = preFrame.getLocal(varIndex)

            val infPostFrame = postFrame as InferenceFrame<QualifiedValueSet<Q>>
            val prevLostValue = infPostFrame.lostValue
            val currLostValue = if (lhs != null) QualifiedValueSet(lhs._size, HashSet(lhs.values)) else null
            val newLostValue =
                    if (currLostValue == null) prevLostValue
                    else if (prevLostValue == null) currLostValue
                    else analyzer.interpreter.merge(prevLostValue, currLostValue)
            infPostFrame.lostValue = newLostValue

            postFrame.setLocal(varIndex, if (rhs != null) QualifiedValueSet(rhs._size, HashSet(rhs.values)) else null)

            return postFrame
        }
        return executedFrame
    }
}

class MultiFrameTransformer<K: AnalysisType, V: CopyableValue<V>>(
        val transformers: Map<K, FrameTransformer<V>>
): FrameTransformer<V> {
    public override fun getPseudoResults(
            insnNode: AbstractInsnNode,
            preFrame: Frame<V>,
            executedFrame: Frame<V>,
            analyzer: Analyzer<V>
    ): Collection<ResultFrame<V>> {
        val preFrameCopy = preFrame.copy()

        val resultList = ArrayList<ResultFrame<V>>()
        for ((key, transformer) in transformers) {
            resultList.addAll(transformer.getPseudoResults(insnNode, preFrameCopy, executedFrame, analyzer))
        }
        return resultList
    }

    public override fun getPostFrame(
            insnNode: AbstractInsnNode,
            edgeKind: EdgeKind,
            preFrame: Frame<V>,
            executedFrame: Frame<V>,
            analyzer: Analyzer<V>
    ): Frame<V>? {
        val preFrameCopy = preFrame.copy()

        var postFrame: Frame<V> = executedFrame
        for ((key, transformer) in transformers) {
            val nextFrame = transformer.getPostFrame(insnNode, edgeKind, preFrameCopy, postFrame, analyzer)
            if (nextFrame == null) {
                return null
            }
            postFrame = nextFrame
        }
        return postFrame
    }
}

fun <V: Value> Analyzer<V>.getInstructionIndex(insn: AbstractInsnNode): Int {
    val m = methodNode
    return if (m != null) m.instructions.indexOf(insn) else -1
}

fun <V: Value> Analyzer<V>.getInstructionFrame(insn: AbstractInsnNode): Frame<V>? {
    val m = methodNode
    return if (m != null) frames[m.instructions.indexOf(insn)] else null
}

fun <V: Value> Frame<V>.getStackFromTop(index: Int): V? =
        getStack(stackSize - index - 1)

fun <V: Value> MethodInsnNode.getReceiver(frame: Frame<V>): V? =
        frame.getStackFromTop(getArgumentCount())

public class QualifiedValuesAnalyzer<Q: Qualifier>(
        val owner: ClassName,
        methodNode: MethodNode,
        val qualifierSet: QualifierSet<Q>,
        val frameTransformer: FrameTransformer<QualifiedValueSet<Q>>,
        val qualifierEvaluator: QualifierEvaluator<Q>
) : Analyzer<QualifiedValueSet<Q>>(QualifiedValuesInterpreter(Method(owner, methodNode), qualifierSet, qualifierEvaluator), frameTransformer) {
    protected override fun newFrame(nLocals: Int, nStack: Int): Frame<QualifiedValueSet<Q>>? {
        return InferenceFrame(nLocals, nStack)
    }

    protected override fun newFrame(src: Frame<out QualifiedValueSet<Q>>): Frame<QualifiedValueSet<Q>> {
        return InferenceFrame(src)
    }
}

public fun <Q: Qualifier> MethodNode.runQualifierAnalysis(
        owner: ClassName,
        qualifierSet: QualifierSet<Q>,
        frameTransformer: FrameTransformer<QualifiedValueSet<Q>>,
        qualifierEvaluator: QualifierEvaluator<Q>
): AnalysisResult<QualifiedValueSet<Q>> =
        QualifiedValuesAnalyzer(owner, this, qualifierSet, frameTransformer, qualifierEvaluator).analyze(owner.internal, this)