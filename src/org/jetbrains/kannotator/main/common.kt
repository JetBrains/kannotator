package org.jetbrains.kannotator.main

import java.io.File
import java.io.FileReader
import java.util.HashMap
import java.util.LinkedHashSet
import kotlinlib.*
import org.jetbrains.kannotator.annotations.io.parseAnnotations
import org.jetbrains.kannotator.asm.util.createMethodNode
import org.jetbrains.kannotator.controlFlow.ControlFlowGraph
import org.jetbrains.kannotator.declarations.*
import org.jetbrains.kannotator.funDependecy.buildFunctionDependencyGraph
import org.jetbrains.kannotator.funDependecy.getTopologicallySortedStronglyConnectedComponents
import org.jetbrains.kannotator.index.AnnotationKeyIndex
import org.jetbrains.kannotator.index.DeclarationIndex
import org.jetbrains.kannotator.index.DeclarationIndexImpl
import org.jetbrains.kannotator.index.FileBasedClassSource
import org.jetbrains.kannotator.index.FieldDependencyInfo
import org.objectweb.asm.tree.MethodNode
import org.jetbrains.kannotator.index.buildFieldsDependencyInfos
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
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.Type

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

private fun List<AnnotationNode>.extractClassNamesTo(classNames: MutableSet<String>) {
    classNames.addAll(this.map{node ->
        Type.getType(node.desc).getClassName()!!}
    )
}

private fun <K> loadInternalAnnotations(
        methodNodes: Map<Method, MethodNode>,
        inferrers: Map<K, AnnotationInferrer<Any>>
): Map<K, Annotations<Any>> {
    val internalAnnotationsMap = inferrers.mapValues { entry -> AnnotationsImpl<Any>() }

    for ((method, methodNode) in methodNodes) {

        PositionsForMethod(method).forEachValidPosition {
            position ->
            val declPos = position.relativePosition
            val classNames =
            when (declPos) {
                RETURN_TYPE -> {
                    val classNames = HashSet<String>()
                    methodNode.visibleAnnotations?.extractClassNamesTo(classNames)
                    methodNode.invisibleAnnotations?.extractClassNamesTo(classNames)
                    classNames
                }
                is ParameterPosition -> {
                    val classNames = HashSet<String>()
                    val index = if (method.isStatic()) declPos.index else declPos.index - 1
                    if (methodNode.visibleParameterAnnotations != null && index < methodNode.visibleParameterAnnotations!!.size) {
                        methodNode.visibleParameterAnnotations!![index]?.extractClassNamesTo(classNames)
                    }

                    if (methodNode.invisibleParameterAnnotations != null && index < methodNode.invisibleParameterAnnotations!!.size) {
                        methodNode.invisibleParameterAnnotations!![index]?.extractClassNamesTo(classNames)
                    }
                    classNames
                }
                else -> Collections.emptySet<String>()
            }

            if (!classNames.empty) {
                for ((inferrerKey, inferrer) in inferrers) {
                    val internalAnnotations = internalAnnotationsMap[inferrerKey]!!
                    val annotation = inferrer.resolveAnnotation(classNames)
                    if (annotation != null) {
                        internalAnnotations[position] = annotation
                    }
                }
            }
        }

    }

    return internalAnnotationsMap
}

private fun <K> loadExternalAnnotations(
        delegatingAnnotations: Map<K, Annotations<Any>>,
        annotationFiles: Collection<File>,
        keyIndex: AnnotationKeyIndex,
        inferrers: Map<K, AnnotationInferrer<Any>>,
        showErrorIfPositionNotFound: Boolean = true
): Map<K, Annotations<Any>> {
    val externalAnnotationsMap = inferrers.mapValues { entry -> AnnotationsImpl<Any>(delegatingAnnotations[entry.key]) }

    for (annotationFile in annotationFiles) {
        FileReader(annotationFile) use {
            parseAnnotations(it, {
                key, annotations ->
                val position = keyIndex.findPositionByAnnotationKeyString(key)
                if (position != null) {
                    val classNames = annotations.mapTo(HashSet<String>(), {data -> data.annotationClassFqn})
                    for ((inferrerKey, inferrer) in inferrers) {
                        val externalAnnotations = externalAnnotationsMap[inferrerKey]!!
                        val annotation = inferrer.resolveAnnotation(classNames)
                        if (annotation != null) {
                            externalAnnotations[position] = annotation
                        }
                    }
                } else if (showErrorIfPositionNotFound) {
                    error("Position not found for $key")
                }
            }, {error(it)})
        }
    }

    return externalAnnotationsMap
}

private fun <K> loadAnnotations(
        annotationFiles: Collection<File>,
        keyIndex: AnnotationKeyIndex,
        methodNodes: Map<Method, MethodNode>,
        inferrers: Map<K, AnnotationInferrer<Any>>,
        showErrorIfPositionNotFound: Boolean = true
): Map<K, Annotations<Any>> =
        loadExternalAnnotations(loadInternalAnnotations(methodNodes, inferrers), annotationFiles, keyIndex, inferrers, showErrorIfPositionNotFound)

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

    fun subsumes(position: AnnotationPosition, parentValue: A, childValue: A): Boolean
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

    val existingAnnotationsMap = loadAnnotations(existingAnnotationFiles, declarationIndex, methodNodes, inferrers, showErrors)
    val resultingAnnotationsMap = existingAnnotationsMap.mapValues{
        entry -> AnnotationsImpl(entry.value)
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

data class AnnotationsConflict<out V>(val position: AnnotationPosition, val expectedValue: V, val actualValue: V)

fun <A: Any> findAnnotationInferenceConflicts(
        inferredAnnotations: Annotations<A>,
        inferrer: AnnotationInferrer<A>
): List<AnnotationsConflict<A>> {
    val existingAnnotations = (inferredAnnotations as AnnotationsImpl<A>).delegate
    if (existingAnnotations != null) {
        val conflicts = ArrayList<AnnotationsConflict<A>>()
        val positions = HashSet<AnnotationPosition>()
        existingAnnotations forEach {
            position, ann -> positions.add(position)
        }
        for (position in positions) {
            val inferred = inferredAnnotations[position]!!
            val existing = existingAnnotations[position]!!
            if (inferred == existing) {
                continue
            }
            if (!inferrer.subsumes(position, existing, inferred)) {
                conflicts.add(AnnotationsConflict(position, existing, inferred))
            }
        }
        return conflicts
    }
    return Collections.emptyList()
}

fun buildControlFlowGraphs(methods: Collection<Method>, node: (Method) -> MethodNode): Map<Method, ControlFlowGraph> {
    return methods.map {m -> Pair(m, node(m).buildControlFlowGraph(m.declaringClass))}.toMap()
}

fun error(message: String) {
    System.err.println(message)
}

