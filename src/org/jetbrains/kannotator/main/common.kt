package org.jetbrains.kannotator.main

import java.io.File
import java.io.FileReader
import java.util.HashMap
import java.util.LinkedHashSet
import kotlinlib.*
import org.jetbrains.kannotator.annotations.io.parseAnnotations
import org.jetbrains.kannotator.asm.util.createMethodNodeStub
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
import org.jetbrains.kannotator.funDependecy.DependencyGraph
import org.jetbrains.kannotator.funDependecy.DependencyNode
import java.util.HashSet
import org.jetbrains.kannotator.controlFlow.builder.buildControlFlowGraph
import java.util.TreeMap
import org.jetbrains.kannotator.annotations.io.toAnnotationKey
import kotlin.nullable.all
import org.jetbrains.kannotator.declarations.copyAllChanged
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.Type
import org.jetbrains.kannotator.declarations.FieldTypePosition
import org.jetbrains.kannotator.declarations.ClassMember
import org.jetbrains.kannotator.index.ClassSource
import java.io.BufferedReader
import org.objectweb.asm.tree.FieldNode
import org.jetbrains.kannotator.index.loadMethodParameterNames
import org.jetbrains.kannotator.classHierarchy.buildClassHierarchyGraph
import org.jetbrains.kannotator.classHierarchy.buildMethodHierarchy
import org.jetbrains.kannotator.annotationsInference.propagation.*

open class ProgressMonitor {
    open fun annotationIndexLoaded(index: AnnotationKeyIndex) {}

    open fun totalFields(fieldCount: Int) {}
    open fun valueFieldsProcessingStarted() {}
    open fun valueFieldsProcessingFinished() {}

    open fun totalMethods(methodCount: Int) {}
    open fun processingStarted(methods: Collection<Method>) {}
    open fun processingStepStarted(method: Method) {}
    open fun processingStepFinished(method: Method) {}
    open fun processingFinished(methods: Collection<Method>) {}
}

private fun List<AnnotationNode?>.extractClassNamesTo(classNames: MutableSet<String>) {
    classNames.addAll(this.filterNotNull().map{node ->
        Type.getType(node.desc).getClassName()!!}
    )
}

public fun <K> loadMethodAnnotationsFromByteCode(
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
                        methodNode.visibleParameterAnnotations!![index]?.filterNotNull()?.extractClassNamesTo(classNames)
                    }

                    if (methodNode.invisibleParameterAnnotations != null && index < methodNode.invisibleParameterAnnotations!!.size) {
                        methodNode.invisibleParameterAnnotations!![index]?.filterNotNull()?.extractClassNamesTo(classNames)
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

public fun <K> loadFieldAnnotationsFromByteCode(
        fieldNodes: Map<Field, FieldNode>,
        inferrers: Map<K, AnnotationInferrer<Any>>
): Map<K, Annotations<Any>> {
    val internalAnnotationsMap = inferrers.mapValues { entry -> AnnotationsImpl<Any>() }

    for ((field, node) in fieldNodes) {
        val position = getFieldTypePosition(field)

        val classNames = HashSet<String>()
        node.visibleAnnotations?.extractClassNamesTo(classNames)
        node.invisibleAnnotations?.extractClassNamesTo(classNames)

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

    return internalAnnotationsMap
}

private fun <K> loadExternalAnnotations(
        delegatingAnnotations: Map<K, Annotations<Any>>,
        annotationFiles: Collection<File>,
        keyIndex: AnnotationKeyIndex,
        inferrers: Map<K, AnnotationInferrer<Any>>,
        showErrorIfPositionNotFound: Boolean = true
): Map<K, MutableAnnotations<Any>> {
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
): Map<K, MutableAnnotations<Any>> =
        loadExternalAnnotations(loadMethodAnnotationsFromByteCode(methodNodes, inferrers), annotationFiles, keyIndex, inferrers, showErrorIfPositionNotFound)

trait AnnotationInferrer<A: Any> {
    fun resolveAnnotation(classNames: Set<String>): A?

    fun inferAnnotationsFromFieldValue(field: Field): Annotations<A>

    fun inferAnnotationsFromMethod(
            method: Method,
            cfGraph: ControlFlowGraph,
            fieldDependencyInfoProvider: (Field) -> FieldDependencyInfo,
            declarationIndex: DeclarationIndex,
            annotations: Annotations<A>): Annotations<A>

    val lattice: AnnotationLattice<A>
}

data class InferenceResult<K>(
        val existingAnnotationsMap: Map<K, Annotations<Any>>,
        val inferredAnnotationsMap: Map<K, Annotations<Any>>
)

fun <K> inferAnnotations(
        classSource: ClassSource,
        existingAnnotationFiles: Collection<File>,
        inferrers: Map<K, AnnotationInferrer<Any>>,
        progressMonitor: ProgressMonitor = ProgressMonitor(),
        showErrors: Boolean = true,
        loadOnly: Boolean = false,
        propagationOverrides: Map<K, Annotations<Any>> = Collections.emptyMap(),
        existingAnnotations: Map<K, Annotations<Any>> = hashMap()
): InferenceResult<K> {
    val methodNodes = HashMap<Method, MethodNode>()
    val declarationIndex = DeclarationIndexImpl(classSource, {
        method ->
        val methodNode = method.createMethodNodeStub()
        methodNodes[method] = methodNode
        methodNode
    })

    progressMonitor.annotationIndexLoaded(declarationIndex)

    val loadedAnnotationsMap = loadAnnotations(existingAnnotationFiles, declarationIndex, methodNodes, inferrers, showErrors)
    val resultingAnnotationsMap = loadedAnnotationsMap.mapValues {entry -> AnnotationsImpl<Any>(entry.value)}
    for (key in inferrers.keySet()) {
        val inferrerExistingAnnotations = existingAnnotations[key]
        if (inferrerExistingAnnotations != null) {
            resultingAnnotationsMap[key]!!.copyAllChanged(inferrerExistingAnnotations)
        }
    }

    val inferenceResult = InferenceResult(loadedAnnotationsMap, resultingAnnotationsMap)

    if (loadOnly) {
        return inferenceResult
    }

    val fieldToDependencyInfosMap = buildFieldsDependencyInfos(declarationIndex, classSource)
    val methodGraph = buildFunctionDependencyGraph(declarationIndex, classSource)
    val components = methodGraph.getTopologicallySortedStronglyConnectedComponents().reverse()

    progressMonitor.totalFields(fieldToDependencyInfosMap.size)

    progressMonitor.valueFieldsProcessingStarted()

    for ((key, inferrer) in inferrers) {
        for (fieldInfo in fieldToDependencyInfosMap.values()) {
            resultingAnnotationsMap[key]!!.copyAllChanged(inferrer.inferAnnotationsFromFieldValue(fieldInfo.field))
        }
    }

    progressMonitor.valueFieldsProcessingFinished()

    progressMonitor.totalMethods(methodNodes.size)

    for (component in components) {
        val methods = component.map { Pair(it.data, it.incomingEdges) }.toMap()
        progressMonitor.processingStarted(methods.keySet())

        fun dependentMembersInsideThisComponent(method: Method): Collection<Method> {
            // Add itself as inferred annotation can produce more annotations
            methods.keySet().intersect(methods.getOrThrow(method).map {e -> e.from.data}).plus(method)
        }

        val methodToGraph = buildControlFlowGraphs(methods.keySet(), { m -> methodNodes.getOrThrow(m) })

        for (method in methods.keySet()) {
            loadMethodParameterNames(method, methodNodes[method]!!)
        }

        for ((key, inferrer) in inferrers) {
            inferAnnotationsOnMutuallyRecursiveMethods(
                    declarationIndex,
                    resultingAnnotationsMap[key]!!,
                    methods.keySet(),
                    { classMember -> dependentMembersInsideThisComponent(classMember) },
                    { m -> methodToGraph.getOrThrow(m) },
                    { f -> fieldToDependencyInfosMap.getOrThrow(f) },
                    inferrer,
                    progressMonitor
            )
        }

        progressMonitor.processingFinished(methods.keySet())

        // We don't need to occupy that memory any more
        for (functionNode in component) {
            methodNodes.remove(functionNode.data)
        }
    }

    return propagateAnnotations(classSource, inferrers, inferenceResult, propagationOverrides)
}

private fun <K> propagateAnnotations(
        classSource: ClassSource,
        inferrers: Map<K, AnnotationInferrer<Any>>,
        inferenceResult: InferenceResult<K>,
        propagationOverrides: Map<K, Annotations<Any>>
): InferenceResult<K> {
    val classHierarchy = buildClassHierarchyGraph(classSource)
    val methodHierarchy = buildMethodHierarchy(classHierarchy)

    val propagatedAnnotations = inferenceResult.inferredAnnotationsMap.mapValues { e ->
        val (key, annotations) = e
        propagateMetadata(methodHierarchy, inferrers[key]!!.lattice, annotations, propagationOverrides[key]!!)
    }

    return inferenceResult.copy(inferredAnnotationsMap = propagatedAnnotations)
}

private fun <A> inferAnnotationsOnMutuallyRecursiveMethods(
        declarationIndex: DeclarationIndex,
        annotations: MutableAnnotations<A>,
        methods: Collection<Method>,
        dependentMethods: (Method) -> Collection<Method>,
        cfGraph: (Method) -> ControlFlowGraph,
        fieldDependencyInfoProvider: (Field) -> FieldDependencyInfo,
        inferrer: AnnotationInferrer<A>,
        progressMonitor: ProgressMonitor
) {
    assert (!methods.isEmpty()) {"Empty SSC"}

    val queue = LinkedHashSet(methods)
    while (!queue.isEmpty()) {
        val method = queue.removeFirst()

        progressMonitor.processingStepStarted(method)

        val inferredAnnotations = inferrer.inferAnnotationsFromMethod(
                method, cfGraph(method), fieldDependencyInfoProvider, declarationIndex, annotations)

        var changed = false
        annotations.copyAllChanged(inferredAnnotations) { pos, previous, new ->
            changed = true
            new // Return merged
        }

        if (changed) {
            queue.addAll(dependentMethods(method))
        }

        progressMonitor.processingStepFinished(method)
    }
}

fun loadPositionsOfConflictExceptions(
        keyIndex: AnnotationKeyIndex,
        exceptionFile: File): Set<AnnotationPosition> {
    return if (exceptionFile.exists() && exceptionFile.canRead()) {
        BufferedReader(FileReader(exceptionFile)) use {br ->
            val positions = HashSet<AnnotationPosition>()
            for (key in br.lineIterator()) {
                if (key.startsWith('#')) {
                    continue
                }
                val pos = keyIndex.findPositionByAnnotationKeyString(key)
                if (pos != null) {
                    positions.add(pos)
                }
            }
            positions
        }
    } else {
        Collections.emptySet<AnnotationPosition>()
    }
}

data class AnnotationsConflict<out V>(val position: AnnotationPosition, val expectedValue: V, val actualValue: V)

fun <A: Any> processAnnotationInferenceConflicts(
        inferredAnnotations: MutableAnnotations<A>,
        existingAnnotations: Annotations<A>?,
        inferrer: AnnotationInferrer<A>,
        positionsOfConflictExceptions: Set<AnnotationPosition> = Collections.emptySet()
): List<AnnotationsConflict<A>> {
    if (existingAnnotations == null) {
        return Collections.emptyList()
    }

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
        if (!inferrer.lattice.subsumes(position.relativePosition, existing, inferred)) {
            if (positionsOfConflictExceptions.contains(position)) {
                inferredAnnotations[position] = existing
            } else {
                conflicts.add(AnnotationsConflict(position, existing, inferred))
            }
        }
    }
    return conflicts
}

fun buildControlFlowGraphs(methods: Collection<Method>, node: (Method) -> MethodNode): Map<Method, ControlFlowGraph> {
    return methods.map {m -> Pair(m, node(m).buildControlFlowGraph(m.declaringClass))}.toMap()
}

fun error(message: String) {
    System.err.println(message)
}

