package org.jetbrains.kannotator.annotationsInference.engine

import org.objectweb.asm.tree.analysis.Value
import org.objectweb.asm.tree.analysis.Frame

public data class AnalysisResult<V: Value>(
        val mergedFrames: Array<Frame<V>?>,
        val returnedResults: List<ResultFrame<V>>,
        val errorResults: List<ResultFrame<V>>
)
