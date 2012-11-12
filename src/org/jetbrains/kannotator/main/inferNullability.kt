package org.jetbrains.kannotator.main

import java.io.File
import java.io.FileReader
import java.util.HashMap
import java.util.LinkedHashSet
import kotlinlib.*
import org.jetbrains.kannotator.annotations.io.parseAnnotations
import org.jetbrains.kannotator.annotationsInference.nullability.NullabilityAnnotation
import org.jetbrains.kannotator.annotationsInference.nullability.buildMethodNullabilityAnnotations
import org.jetbrains.kannotator.annotationsInference.nullability.classNamesToNullabilityAnnotation
import org.jetbrains.kannotator.asm.util.createMethodNode
import org.jetbrains.kannotator.controlFlow.ControlFlowGraph
import org.jetbrains.kannotator.controlFlow.builder.buildControlFlowGraph
import org.jetbrains.kannotator.declarations.Annotations
import org.jetbrains.kannotator.declarations.AnnotationsImpl
import org.jetbrains.kannotator.declarations.Method
import org.jetbrains.kannotator.declarations.MutableAnnotations
import org.jetbrains.kannotator.declarations.PositionsForMethod
import org.jetbrains.kannotator.funDependecy.buildFunctionDependencyGraph
import org.jetbrains.kannotator.funDependecy.getTopologicallySortedStronglyConnectedComponents
import org.jetbrains.kannotator.index.AnnotationKeyIndex
import org.jetbrains.kannotator.index.DeclarationIndex
import org.jetbrains.kannotator.index.DeclarationIndexImpl
import org.jetbrains.kannotator.index.FileBasedClassSource
import org.objectweb.asm.tree.MethodNode
import org.jetbrains.kannotator.index.buildFieldsDependencyInfos
import org.jetbrains.kannotator.annotationsInference.nullability.buildFieldNullabilityAnnotations
import org.jetbrains.kannotator.declarations.isStatic
import org.jetbrains.kannotator.declarations.isFinal

open class ProgressMonitor {
    open fun totalMethods(methodCount: Int) {}
    open fun processingStarted(methods: Collection<Method>) {}
    open fun processingStepStarted(method: Method) {}
    open fun processingStepFinished(method: Method) {}
    open fun processingFinished(methods: Collection<Method>) {}
}

fun inferNullabilityAnnotations(
        jarOrClassFiles: Collection<File>,
        existingAnnotationFiles: Collection<File>,
        progressMonitor: ProgressMonitor = ProgressMonitor()
): Annotations<NullabilityAnnotation> {
    val classSource = FileBasedClassSource(jarOrClassFiles)

    val methodNodes = HashMap<Method, MethodNode>()
    val declarationIndex = DeclarationIndexImpl(classSource) {
        method ->
        val methodNode = method.createMethodNode()
        methodNodes[method] = methodNode
        methodNode
    }

    progressMonitor.totalMethods(methodNodes.size)

    // TODO Load annotations from .class files too, see MethodNode.visibleParameterAnnotations and MethodNode.invisibleParameterAnnotations
    val existingNullabilityAnnotations = loadNullabilityAnnotations(existingAnnotationFiles, declarationIndex)

    val resultingAnnotations = AnnotationsImpl(existingNullabilityAnnotations)

    val fieldsInfosMap = buildFieldsDependencyInfos(declarationIndex, classSource)
    for (fieldInfo in fieldsInfosMap.values()) {
        val methodToGraph = buildControlFlowGraphs(fieldInfo.writers, { m -> methodNodes.getOrThrow(m) })
        val inferredAnnotations = buildFieldNullabilityAnnotations(fieldInfo, { m -> methodToGraph.getOrThrow(m) }, declarationIndex, resultingAnnotations)

        // Store
        inferredAnnotations forEach { pos, ann ->
            val present = resultingAnnotations[pos]
            if (present != ann) {
                resultingAnnotations[pos] = ann
            }
        }
    }

    val methodGraph = buildFunctionDependencyGraph(declarationIndex, classSource)
    val components = methodGraph.getTopologicallySortedStronglyConnectedComponents().reverse()

    for (component in components) {
        val methods = component.map { Pair(it.method, it.incomingEdges) }.toMap()
        progressMonitor.processingStarted(methods.keySet())

        fun dependentMembersInsideThisComponent(m: Method): Collection<Method> {
            return methods.getOrThrow(m)
                    .map {e -> e.from.method} // dependent members
                    .filter {m -> m in methods.keySet()} // only inside this component
        }

        val methodToGraph = buildControlFlowGraphs(methods.keySet(), { m -> methodNodes.getOrThrow(m) })

        inferAnnotationsOnMutuallyRecursiveMethods(
                declarationIndex,
                resultingAnnotations,
                methods.keySet(),
                { m -> dependentMembersInsideThisComponent(m) },
                { m -> methodToGraph.getOrThrow(m) },
                progressMonitor
        )

        progressMonitor.processingFinished(methods.keySet())

        // We don't need to occupy that memory any more
        for (functionNode in component) {
             methodNodes.remove(functionNode.method)
        }
    }
    return resultingAnnotations
}

private fun inferAnnotationsOnMutuallyRecursiveMethods(
        declarationIndex: DeclarationIndex,
        annotations: MutableAnnotations<NullabilityAnnotation>,
        methods: Collection<Method>,
        dependentMethods: (Method) -> Collection<Method>,
        cfGraph: (Method) -> ControlFlowGraph,
        progressMonitor: ProgressMonitor

) {
    assert (!methods.isEmpty()) {"Empty SSC"}

    val queue = LinkedHashSet(methods)
    while (!queue.isEmpty()) {
        val method = queue.removeFirst()

        progressMonitor.processingStepStarted(method)
        val inferredAnnotations = buildMethodNullabilityAnnotations(cfGraph(method), PositionsForMethod(method), declarationIndex, annotations)
        progressMonitor.processingStepFinished(method)

        var changed = false
        inferredAnnotations forEach {
            pos, ann ->
            val present = annotations[pos]
            if (present != ann) {
                annotations[pos] = ann
                changed = true
            }
        }

        if (changed) {
            queue.addAll(dependentMethods(method))
        }
    }
}

fun loadNullabilityAnnotations(
        annotationFiles: Collection<File>,
        keyIndex: AnnotationKeyIndex): Annotations<NullabilityAnnotation>
{
    val nullabilityAnnotations = AnnotationsImpl<NullabilityAnnotation>()

    for (annotationFile in annotationFiles) {
        FileReader(annotationFile) use {
            parseAnnotations(it, {
                key, annotations ->
                val position = keyIndex.findPositionByAnnotationKeyString(key)
                if (position == null) {
                    error("Position not found for $key")
                }
                else {
                    for (data in annotations) {
                        val annotation = classNamesToNullabilityAnnotation(hashSet(data.annotationClassFqn))
                        if (annotation != null) {
                            nullabilityAnnotations[position] = annotation
                        }
                    }
                }
            }, {error(it)})
        }
    }

    return nullabilityAnnotations
}

fun buildControlFlowGraphs(methods: Collection<Method>, node: (Method) -> MethodNode): Map<Method, ControlFlowGraph> {
    return methods.map {m -> Pair(m, node(m).buildControlFlowGraph(m.declaringClass))}.toMap()
}

fun error(message: String) {
    System.err.println(message)
}