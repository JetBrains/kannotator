package org.jetbrains.kannotator.controlFlow.builder.analysis

import java.util.HashMap
import kotlinlib.mapMerge
import kotlinlib.mapValues
import org.jetbrains.kannotator.runtime.annotations.AnalysisType

public interface Qualifier

public interface QualifierSet<Q: Qualifier> {
    public val id: AnalysisType
    public val initial: Q

    public fun merge(q1: Q, q2: Q): Q

    public fun contains(q: Qualifier): Boolean
}

public interface QualifierEvaluator<out Q: Qualifier> {
    fun evaluateQualifier(baseValue: TypedValue): Q
}

public class MultiQualifier<K: AnalysisType>(val qualifiers: Map<K, Qualifier>): Qualifier {
    public fun copy(key: K, qualifier: Qualifier): MultiQualifier<K> {
        val map = HashMap<K, Qualifier>(qualifiers)
        map.put(key, qualifier)
        return MultiQualifier(map)
    }

    override fun toString() = qualifiers.values().toString()

    override fun equals(other: Any?): Boolean {
        if (this identityEquals other) return true
        return other is MultiQualifier<*> && qualifiers == other.qualifiers
    }

    override fun hashCode(): Int = qualifiers.hashCode()
}

public class MultiQualifierSet<K: AnalysisType>(val qualifierSets: Map<K, QualifierSet<Qualifier>>): QualifierSet<MultiQualifier<K>> {
    companion object {
        private object MULTI_QUALIFIER_KEY : AnalysisType
    }

    public override val id: AnalysisType = MULTI_QUALIFIER_KEY

    public override val initial: MultiQualifier<K> =
            MultiQualifier(qualifierSets.mapValues { key, qualifierSet -> qualifierSet.initial })

    public override fun merge(q1: MultiQualifier<K>, q2: MultiQualifier<K>): MultiQualifier<K> {
        val map = mapMerge(q1.qualifiers, q2.qualifiers) { key, v1, v2 ->
            qualifierSets[key]!!.merge(v1, v2)
        }
        return MultiQualifier(map)
    }

    public override fun contains(q: Qualifier): Boolean = q is MultiQualifier<*>
}

public class MultiQualifierEvaluator<K: AnalysisType>(
        val evaluators: Map<K, QualifierEvaluator<*>>
): QualifierEvaluator<MultiQualifier<K>> {
    override fun evaluateQualifier(baseValue: TypedValue): MultiQualifier<K> {
        return MultiQualifier(evaluators.mapValues { key, eval -> eval.evaluateQualifier(baseValue) })
    }
}