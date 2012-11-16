package org.jetbrains.kannotator.main

import java.io.File
import java.io.FileReader
import java.util.HashMap
import java.util.LinkedHashSet
import kotlinlib.*
import org.jetbrains.kannotator.annotations.io.parseAnnotations
import org.jetbrains.kannotator.asm.util.createMethodNode
import org.jetbrains.kannotator.controlFlow.ControlFlowGraph
import org.jetbrains.kannotator.declarations.AnnotationPosition
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
import org.jetbrains.kannotator.index.FieldDependencyInfo
import org.objectweb.asm.tree.MethodNode
import org.jetbrains.kannotator.index.buildFieldsDependencyInfos
import org.jetbrains.kannotator.declarations.isStatic
import org.jetbrains.kannotator.declarations.isFinal
import org.jetbrains.kannotator.declarations.Field
import java.util.Collections
import org.jetbrains.kannotator.annotationsInference.Annotation
import java.util.ArrayList
import org.jetbrains.kannotator.funDependecy.FunDependencyGraph
import org.jetbrains.kannotator.funDependecy.FunctionNode
import java.util.HashSet
import org.jetbrains.kannotator.controlFlow.builder.buildControlFlowGraph
import java.util.TreeMap
import org.jetbrains.kannotator.annotations.io.toAnnotationKey
import kotlin.nullable.all

open class ProgressMonitor {
    open fun totalFields(fieldCount: Int) {}
    open fun processingStepStarted(field: Field) {}
    open fun processingStepFinished(field: Field) {}

    open fun totalMethods(methodCount: Int) {}
    open fun processingStarted(methods: Collection<Method>) {}
    open fun processingStepStarted(method: Method) {}
    open fun processingStepFinished(method: Method) {}
    open fun processingFinished(methods: Collection<Method>) {}
}

fun <A> loadAnnotations(
        annotationFiles: Collection<File>,
        keyIndex: AnnotationKeyIndex,
        inferrer: AnnotationInferrer<A>,
        showErrorIfPositionNotFound: Boolean = true
): Annotations<A> {
    val resolvedAnnotations = AnnotationsImpl<A>()

    for (annotationFile in annotationFiles) {
        FileReader(annotationFile) use {
            parseAnnotations(it, {
                key, annotations ->
                val position = keyIndex.findPositionByAnnotationKeyString(key)
                if (position != null) {
                    for (data in annotations) {
                        val annotation = inferrer.resolveAnnotation(hashSet(data.annotationClassFqn))

                        if (annotation != null) {
                            resolvedAnnotations[position] = annotation
                        }
                    }
                } else if (showErrorIfPositionNotFound) {
                    error("Position not found for $key")
                }
            }, {error(it)})
        }
    }

    return resolvedAnnotations
}

trait AnnotationInferrer<A: Any> {
    fun resolveAnnotation(classNames: Set<String>): A?

    val supportsFields: Boolean
    fun inferFieldAnnotations(
            fieldInfo: FieldDependencyInfo,
            controlFlowGraphBuilder: (Method) -> ControlFlowGraph,
            declarationIndex: DeclarationIndex,
            annotations: Annotations<A>): Annotations<A>

    val supportsMethods: Boolean
    fun inferMethodAnnotations(
            graph: ControlFlowGraph,
            positions: PositionsForMethod,
            declarationIndex: DeclarationIndex,
            annotations: Annotations<A>): Annotations<A>
}

fun <K> inferAnnotations(
        jarOrClassFiles: Collection<File>,
        existingAnnotationFiles: Collection<File>,
        inferrers: Map<K, AnnotationInferrer<Any>>,
        progressMonitor: ProgressMonitor = ProgressMonitor(),
        showErrors: Boolean = true
): Map<K, Annotations<Any>> {
    val classSource = FileBasedClassSource(jarOrClassFiles)

    val methodNodes = HashMap<Method, MethodNode>()
    val declarationIndex = DeclarationIndexImpl(classSource) {
        method ->
        val methodNode = method.createMethodNode()
        methodNodes[method] = methodNode
        methodNode
    }

    // TODO Load annotations from .class files too, see MethodNode.visibleParameterAnnotations and MethodNode.invisibleParameterAnnotations
    val resultingAnnotationsMap = inferrers.mapValues {entry ->
        AnnotationsImpl(loadAnnotations(existingAnnotationFiles, declarationIndex, entry.value, showErrors))
    }

    val fieldsInfosMap = buildFieldsDependencyInfos(declarationIndex, classSource)
    val methodGraph = buildFunctionDependencyGraph(declarationIndex, classSource)
    val components = methodGraph.getTopologicallySortedStronglyConnectedComponents().reverse()

    progressMonitor.totalFields(fieldsInfosMap.size)

    for (fieldInfo in fieldsInfosMap.values()) {
        progressMonitor.processingStepStarted(fieldInfo.field)

        val methodToGraph = buildControlFlowGraphs(fieldInfo.writers, { m -> methodNodes.getOrThrow(m) })
        val inferredAnnotationsMap = inferrers.mapValues { entry ->
            val (key, inferrer) = entry
            val resultingAnnotations = resultingAnnotationsMap[key]!!

            if (inferrer.supportsFields)
                inferrer.inferFieldAnnotations(fieldInfo, { m -> methodToGraph.getOrThrow(m) }, declarationIndex, resultingAnnotations)
            else
                null
        }

        for ((key, inferredAnnotations) in inferredAnnotationsMap) {
            if (inferredAnnotations == null)
                continue
            val resultingAnnotations = resultingAnnotationsMap[key]!!
            inferredAnnotations forEach { pos, ann ->
                val present = resultingAnnotations[pos]
                if (present != ann) {
                    resultingAnnotations[pos] = ann
                }
            }
        }

        progressMonitor.processingStepFinished(fieldInfo.field)
    }

    progressMonitor.totalMethods(methodNodes.size)

    for (component in components) {
        val methods = component.map { Pair(it.method, it.incomingEdges) }.toMap()
        progressMonitor.processingStarted(methods.keySet())

        fun dependentMembersInsideThisComponent(m: Method): Collection<Method> {
            return methods.getOrThrow(m)
                    .map {e -> e.from.method} // dependent members
                    .filter {m -> m in methods.keySet()} // only inside this component
        }

        val methodToGraph = buildControlFlowGraphs(methods.keySet(), { m -> methodNodes.getOrThrow(m) })

        for ((key, inferencer) in inferrers) {
            inferAnnotationsOnMutuallyRecursiveMethods(
                    declarationIndex,
                    resultingAnnotationsMap[key]!!,
                    methods.keySet(),
                    { m -> dependentMembersInsideThisComponent(m) },
                    { m -> methodToGraph.getOrThrow(m) },
                    inferencer,
                    progressMonitor
            )
        }

        progressMonitor.processingFinished(methods.keySet())

        // We don't need to occupy that memory any more
        for (functionNode in component) {
            methodNodes.remove(functionNode.method)
        }
    }

    return resultingAnnotationsMap
}

private fun <A> inferAnnotationsOnMutuallyRecursiveMethods(
        declarationIndex: DeclarationIndex,
        annotations: MutableAnnotations<A>,
        methods: Collection<Method>,
        dependentMethods: (Method) -> Collection<Method>,
        cfGraph: (Method) -> ControlFlowGraph,
        inferrer: AnnotationInferrer<A>,
        progressMonitor: ProgressMonitor
) {
    assert (!methods.isEmpty()) {"Empty SSC"}

    val queue = LinkedHashSet(methods)
    while (!queue.isEmpty()) {
        val method = queue.removeFirst()

        progressMonitor.processingStepStarted(method)
        val inferredAnnotations = inferrer.inferMethodAnnotations(cfGraph(method), PositionsForMethod(method), declarationIndex, annotations)
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

fun buildControlFlowGraphs(methods: Collection<Method>, node: (Method) -> MethodNode): Map<Method, ControlFlowGraph> {
    return methods.map {m -> Pair(m, node(m).buildControlFlowGraph(m.declaringClass))}.toMap()
}

fun error(message: String) {
    System.err.println(message)
}