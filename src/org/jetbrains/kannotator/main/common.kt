package org.jetbrains.kannotator.main

import java.io.File
import java.io.FileReader
import java.util.HashMap
import java.util.LinkedHashSet
import kotlinlib.*
import org.jetbrains.kannotator.annotations.io.parseAnnotations
import org.jetbrains.kannotator.asm.util.createMethodNodeStub
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
import org.jetbrains.kannotator.controlFlow.builder.analysis.Annotation
import java.util.ArrayList
import org.jetbrains.kannotator.funDependecy.DependencyGraph
import org.jetbrains.kannotator.funDependecy.DependencyNode
import java.util.HashSet
import java.util.TreeMap
import org.jetbrains.kannotator.annotations.io.toAnnotationKey
import kotlin.all
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
import org.jetbrains.kannotator.controlFlow.builder.analysis.*
import org.jetbrains.kannotator.controlFlow.builder.*
import org.jetbrains.kannotator.annotationsInference.engine.*

open class ProgressMonitor {
    open fun processingStarted() {}
    open fun annotationIndexLoaded(index: AnnotationKeyIndex) {}
    open fun methodsProcessingStarted(methodCount: Int) {}
    open fun processingComponentStarted(methods: Collection<Method>) {}
    open fun processingStepStarted(method: Method) {}
    open fun processingStepFinished(method: Method) {}
    open fun processingComponentFinished(methods: Collection<Method>) {}
    open fun processingFinished() {}
}

private fun List<AnnotationNode?>.extractClassNamesTo(classNames: MutableSet<String>) {
    classNames.addAll(this.filterNotNull().map{node ->
        Type.getType(node.desc).getClassName()!!}
    )
}

public fun <K> loadMethodAnnotationsFromByteCode(
        methodNodes: Map<Method, MethodNode>,
        inferrers: Map<K, AnnotationInferrer<Any, Qualifier>>
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
        inferrers: Map<K, AnnotationInferrer<Any, Qualifier>>
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
        inferrers: Map<K, AnnotationInferrer<Any, Qualifier>>,
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
        inferrers: Map<K, AnnotationInferrer<Any, Qualifier>>,
        showErrorIfPositionNotFound: Boolean = true
): Map<K, MutableAnnotations<Any>> =
        loadExternalAnnotations(loadMethodAnnotationsFromByteCode(methodNodes, inferrers), annotationFiles, keyIndex, inferrers, showErrorIfPositionNotFound)

trait AnnotationInferrer<A: Any, I: Qualifier> {
    fun resolveAnnotation(classNames: Set<String>): A?

    fun inferAnnotationsFromFieldValue(field: Field): Annotations<A>

    fun <Q: Qualifier> inferAnnotationsFromMethod(
            method: Method,
            methodNode: MethodNode,
            analysisResult: AnalysisResult<QualifiedValueSet<Q>>,
            fieldDependencyInfoProvider: (Field) -> FieldDependencyInfo,
            declarationIndex: DeclarationIndex,
            annotations: Annotations<A>): Annotations<A>

    val lattice: AnnotationLattice<A>
    val qualifierSet: QualifierSet<I>

    fun getFrameTransformer(annotations: Annotations<A>, declarationIndex: DeclarationIndex): FrameTransformer<QualifiedValueSet<*>>
    fun getQualifierEvaluator(positions: PositionsForMethod, annotations: Annotations<A>, declarationIndex: DeclarationIndex): QualifierEvaluator<I>
}

data class InferenceResult<K>(
        val existingAnnotationsMap: Map<K, Annotations<Any>>,
        val inferredAnnotationsMap: Map<K, Annotations<Any>>
)

fun <K> inferAnnotations(
        classSource: ClassSource,
        existingAnnotationFiles: Collection<File>,
        inferrers: Map<K, AnnotationInferrer<Any, Qualifier>>,
        progressMonitor: ProgressMonitor = ProgressMonitor(),
        showErrors: Boolean = true,
        loadOnly: Boolean = false,
        propagationOverrides: Map<K, Annotations<Any>>,
        existingAnnotations: Map<K, Annotations<Any>>
): InferenceResult<K> {
    progressMonitor.processingStarted()
    
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

    for ((key, inferrer) in inferrers) {
        for (fieldInfo in fieldToDependencyInfosMap.values()) {
            resultingAnnotationsMap[key]!!.copyAllChanged(inferrer.inferAnnotationsFromFieldValue(fieldInfo.field))
        }
    }

    progressMonitor.methodsProcessingStarted(methodNodes.size)

    for (component in components) {
        val methods = component.map { Pair(it.data, it.incomingEdges) }.toMap()
        progressMonitor.processingComponentStarted(methods.keySet())

        fun dependentMembersInsideThisComponent(method: Method): Collection<Method> {
            // Add itself as inferred annotation can produce more annotations
            methods.keySet().intersect(methods.getOrThrow(method).map {e -> e.from.data}).plus(method)
        }

        for (method in methods.keySet()) {
            loadMethodParameterNames(method, methodNodes[method]!!)
        }

        inferAnnotationsOnMutuallyRecursiveMethods(
                declarationIndex,
                resultingAnnotationsMap,
                methods.keySet(),
                { classMember -> dependentMembersInsideThisComponent(classMember) },
                { m -> methodNodes.getOrThrow(m) },
                { f -> fieldToDependencyInfosMap.getOrThrow(f) },
                inferrers,
                progressMonitor
        )

        progressMonitor.processingComponentFinished(methods.keySet())

        // We don't need to occupy that memory any more
        for (functionNode in component) {
            methodNodes.remove(functionNode.data)
        }
    }

    progressMonitor.processingFinished()

    return propagateAnnotations(classSource, inferrers, inferenceResult, propagationOverrides)
}

private fun <K> propagateAnnotations(
        classSource: ClassSource,
        inferrers: Map<K, AnnotationInferrer<Any, Qualifier>>,
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

private fun <K, A> inferAnnotationsOnMutuallyRecursiveMethods(
        declarationIndex: DeclarationIndex,
        annotationsMap: Map<K, MutableAnnotations<A>>,
        methods: Collection<Method>,
        dependentMethods: (Method) -> Collection<Method>,
        methodNodes: (Method) -> MethodNode,
        fieldDependencyInfoProvider: (Field) -> FieldDependencyInfo,
        inferrers: Map<K, AnnotationInferrer<Any, Qualifier>>,
        progressMonitor: ProgressMonitor
) {
    assert (!methods.isEmpty()) {"Empty SSC"}

    val queue = LinkedHashSet(methods)
    while (!queue.isEmpty()) {
        val method = queue.removeFirst()

        progressMonitor.processingStepStarted(method)

        val analysisResult = methodNodes(method).runQualifierAnalysis<MultiQualifier<Any>>(
                method.declaringClass,
                MultiQualifierSet(inferrers.mapKeysAndValues (
                        {it.value.qualifierSet.id}, {it.value.qualifierSet}
                )),
                MultiFrameTransformer<Any, QualifiedValueSet<Qualifier>>(inferrers.mapKeysAndValues (
                        {it.value.qualifierSet.id},
                        {it.value.getFrameTransformer(annotationsMap[it.key]!!, declarationIndex)}
                )),
                MultiQualifierEvaluator(inferrers.mapKeysAndValues (
                        {it.value.qualifierSet.id},
                        {it.value.getQualifierEvaluator(PositionsForMethod(method), annotationsMap[it.key]!!, declarationIndex)}
                ))
        )

        for ((key, inferrer) in inferrers) {
            val annotations = annotationsMap[key]!!

            val inferredAnnotations = inferrer.inferAnnotationsFromMethod<MultiQualifier<Any>>(
                    method, methodNodes(method), analysisResult, fieldDependencyInfoProvider, declarationIndex, annotations)

            var changed = false
            annotations.copyAllChanged(inferredAnnotations as Annotations<A>) { pos, previous, new ->
                val isParam = pos.relativePosition is ParameterPosition
                if ((isParam && previous == null) || !isParam) {
                    changed = true
                    new
                } else previous!!
            }

            if (changed) {
                queue.addAll(dependentMethods(method))
            }
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
        inferrer: AnnotationInferrer<A, *>,
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

fun error(message: String) {
    System.err.println(message)
}

