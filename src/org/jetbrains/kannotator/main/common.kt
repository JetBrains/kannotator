package org.jetbrains.kannotator.main

import java.io.File
import java.io.FileReader
import java.util.HashMap
import java.util.LinkedHashSet
import kotlinlib.*
import org.jetbrains.kannotator.annotations.io.parseAnnotations
import org.jetbrains.kannotator.asm.util.createMethodNodeStub
import org.jetbrains.kannotator.declarations.*
import org.jetbrains.kannotator.index.AnnotationKeyIndex
import org.jetbrains.kannotator.index.DeclarationIndex
import org.jetbrains.kannotator.index.DeclarationIndexImpl
import org.jetbrains.kannotator.index.FieldDependencyInfo
import org.objectweb.asm.tree.MethodNode
import org.jetbrains.kannotator.index.buildFieldsDependencyInfos
import java.util.Collections
import java.util.ArrayList
import java.util.HashSet
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.Type
import org.jetbrains.kannotator.index.ClassSource
import java.io.BufferedReader
import org.objectweb.asm.tree.FieldNode
import org.jetbrains.kannotator.index.loadMethodParameterNames
import org.jetbrains.kannotator.classHierarchy.buildClassHierarchyGraph
import org.jetbrains.kannotator.classHierarchy.buildMethodHierarchy
import org.jetbrains.kannotator.annotationsInference.propagation.*
import org.jetbrains.kannotator.controlFlow.builder.analysis.*
import org.jetbrains.kannotator.annotationsInference.engine.*
import org.jetbrains.kannotator.funDependecy.*
import org.jetbrains.kannotator.graphs.dependencyGraphs.PackageDependencyGraphBuilder
import org.jetbrains.kannotator.graphs.removeGraphNodes
import org.jetbrains.kannotator.classHierarchy.HierarchyGraph
import org.jetbrains.kannotator.annotations.io.AnnotationData
import org.jetbrains.kannotator.annotations.io.AnnotationDataImpl
import org.jetbrains.kannotator.runtime.annotations.AnalysisType
import org.jetbrains.kannotator.ErrorHandler
import java.io.Reader

open class ProgressMonitor {
    open fun processingStarted() {}
    open fun annotationIndexLoaded(index: AnnotationKeyIndex) {}
    open fun methodsProcessingStarted(methodCount: Int) {}
    open fun processingComponentStarted(methods: Collection<Method>) {}
    open fun processingStepStarted(method: Method) {}
    open fun processingStepFinished(method: Method) {}
    open fun processingComponentFinished(methods: Collection<Method>) {}
    open fun processingFinished() {}
    open fun processingAborted() {}
}

private fun List<AnnotationNode?>.extractAnnotationDataMapTo(annotationsMap: MutableMap<String, AnnotationData>) {
    this.filterNotNull().toMutableMap(annotationsMap){node ->
        val className = Type.getType(node.desc).getClassName()!!
        val attributes = HashMap<String, String>()
        val values = node.values
        if (values != null) {
            for (i in values.indices step 2) {
                attributes[values[i].toString()] = values[i + 1].toString()
            }
        }

        className to AnnotationDataImpl(className, attributes)}
}

public fun <K: AnalysisType> loadMethodAnnotationsFromByteCode(
        methodNodes: Map<Method, MethodNode>,
        inferrers: Map<K, AnnotationInferrer<Any, Qualifier>>
): Map<K, Annotations<Any>> {
    val internalAnnotationsMap = inferrers.mapValues { entry -> AnnotationsImpl<Any>() }

    for ((method, methodNode) in methodNodes) {

        PositionsForMethod(method).forEachValidPosition {
            position ->
            val declPos = position.relativePosition
            val annotationsMap =
            when (declPos) {
                RETURN_TYPE -> {
                    val annotationsMap = HashMap<String, AnnotationData>()
                    methodNode.visibleAnnotations?.extractAnnotationDataMapTo(annotationsMap)
                    methodNode.invisibleAnnotations?.extractAnnotationDataMapTo(annotationsMap)
                    annotationsMap
                }
                is ParameterPosition -> {
                    val annotationsMap = HashMap<String, AnnotationData>()
                    val index = if (method.isStatic()) declPos.index else declPos.index - 1
                    if (methodNode.visibleParameterAnnotations != null && index < methodNode.visibleParameterAnnotations!!.size) {
                        methodNode.visibleParameterAnnotations!![index]?.filterNotNull()?.extractAnnotationDataMapTo(annotationsMap)
                    }

                    if (methodNode.invisibleParameterAnnotations != null && index < methodNode.invisibleParameterAnnotations!!.size) {
                        methodNode.invisibleParameterAnnotations!![index]?.filterNotNull()?.extractAnnotationDataMapTo(annotationsMap)
                    }
                    annotationsMap
                }
                else -> Collections.emptyMap<String, AnnotationData>()
            }

            if (!annotationsMap.empty) {
                for ((inferrerKey, inferrer) in inferrers) {
                    val internalAnnotations = internalAnnotationsMap[inferrerKey]!!
                    val annotation = inferrer.resolveAnnotation(annotationsMap)
                    if (annotation != null) {
                        internalAnnotations[position] = annotation
                    }
                }
            }
        }

    }

    return internalAnnotationsMap
}

public fun <K: AnalysisType> loadFieldAnnotationsFromByteCode(
        fieldNodes: Map<Field, FieldNode>,
        inferrers: Map<K, AnnotationInferrer<Any, Qualifier>>
): Map<K, Annotations<Any>> {
    val internalAnnotationsMap = inferrers.mapValues { entry -> AnnotationsImpl<Any>() }

    for ((field, node) in fieldNodes) {
        val position = getFieldTypePosition(field)

        val annotationsMap = HashMap<String, AnnotationData>()
        node.visibleAnnotations?.extractAnnotationDataMapTo(annotationsMap)
        node.invisibleAnnotations?.extractAnnotationDataMapTo(annotationsMap)

        if (!annotationsMap.empty) {
            for ((inferrerKey, inferrer) in inferrers) {
                val internalAnnotations = internalAnnotationsMap[inferrerKey]!!
                val annotation = inferrer.resolveAnnotation(annotationsMap)
                if (annotation != null) {
                    internalAnnotations[position] = annotation
                }
            }
        }

    }

    return internalAnnotationsMap
}

public fun <K: AnalysisType> loadExternalAnnotations(
        delegatingAnnotations: Map<K, Annotations<Any>>,
        annotationsInXml: Collection<() -> Reader>,
        keyIndex: AnnotationKeyIndex,
        inferrers: Map<K, AnnotationInferrer<Any, Qualifier>>,
        errorHandler: ErrorHandler
): Map<K, MutableAnnotations<Any>> {
    val externalAnnotationsMap = inferrers.mapValues { (key, inferrer) -> AnnotationsImpl<Any>(delegatingAnnotations[key]) }

    for (xml in annotationsInXml) {
        xml() use {
            parseAnnotations(it, {
                key, annotations ->
                val position = keyIndex.findPositionByAnnotationKeyString(key)
                if (position != null) {
                    val classNames = annotations.toMap({data -> data.annotationClassFqn to data})
                    for ((inferrerKey, inferrer) in inferrers) {
                        val externalAnnotations = externalAnnotationsMap[inferrerKey]!!
                        val annotation = inferrer.resolveAnnotation(classNames)
                        if (annotation != null) {
                            externalAnnotations[position] = annotation
                        }
                    }
                } else {
                    errorHandler.error("Position not found for $key")
                }
            }, errorHandler)
        }
    }

    return externalAnnotationsMap
}

private fun <K: AnalysisType> loadAnnotations(
        annotationFiles: Collection<File>,
        keyIndex: AnnotationKeyIndex,
        methodNodes: Map<Method, MethodNode>,
        inferrers: Map<K, AnnotationInferrer<Any, Qualifier>>,
        errorHandler: ErrorHandler
): Map<K, MutableAnnotations<Any>> =
        loadExternalAnnotations(loadMethodAnnotationsFromByteCode(methodNodes, inferrers),
                annotationFiles map { {FileReader(it)} }, keyIndex, inferrers, errorHandler)

trait AnnotationInferrer<A: Any, I: Qualifier> {
    fun resolveAnnotation(classNames: Map<String, AnnotationData>): A?

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

data class InferenceResultGroup<A: Any>(
        val existingAnnotations: Annotations<A>,
        val inferredAnnotations: Annotations<A>,
        val propagatedPositions: Set<AnnotationPosition>
)

data class InferenceResult<K: AnalysisType>(val groupByKey: Map<K, InferenceResultGroup<Any>>)

fun <K: AnalysisType> inferAnnotations(
        classSource: ClassSource,
        existingAnnotationFiles: Collection<File>,
        inferrers: Map<K, AnnotationInferrer<Any, Qualifier>>,
        progressMonitor: ProgressMonitor = ProgressMonitor(),
        errorHandler: ErrorHandler,
        loadOnly: Boolean = false,
        propagationOverrides: Map<K, Annotations<Any>>,
        existingAnnotations: Map<K, Annotations<Any>>,
        packageIsInteresting: (String) -> Boolean,
        existingPositionsToExclude: Map<K, Set<AnnotationPosition>>,
        loadAnnotationsForDependency: (Map<K, MutableAnnotations<Any>>, ClassMember, DeclarationIndexImpl) -> Boolean = {_, __, ___ -> false}
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

    val loadedAnnotationsMap = loadAnnotations(existingAnnotationFiles, declarationIndex, methodNodes, inferrers, errorHandler)
    val filteredLoadedAnnotationsMap = loadedAnnotationsMap.mapValues { (key, loadedAnn) ->
        val positionsToExclude = existingPositionsToExclude[key]

        if (positionsToExclude == null || positionsToExclude.empty) loadedAnn
        else {
            val newAnn = AnnotationsImpl<Any>(loadedAnn.delegate)

            loadedAnn.forEach { (pos, ann) ->
                if (pos !in positionsToExclude) newAnn[pos] = ann
            }

            newAnn
        }
    }

    val resultingAnnotationsMap = filteredLoadedAnnotationsMap.mapValues {(key, ann) -> AnnotationsImpl<Any>(ann)}
    for (key in inferrers.keySet()) {
        val inferrerExistingAnnotations = existingAnnotations[key]
        if (inferrerExistingAnnotations != null) {
            resultingAnnotationsMap[key]!!.copyAllChanged(inferrerExistingAnnotations)
        }
    }

    val inferenceResult = InferenceResult(
            inferrers.mapValues { (key, inferrer) ->
                InferenceResultGroup<Any>(
                        loadedAnnotationsMap[key]!!,
                        resultingAnnotationsMap[key]!!,
                        HashSet<AnnotationPosition>()
                )
            }
    )

    if (loadOnly) {
        return inferenceResult
    }

    val fieldToDependencyInfosMap = buildFieldsDependencyInfos(declarationIndex, classSource)

    val declarationIndexWithDependencies = DeclarationIndexImpl(declarationIndex)
    val methodGraphBuilder = FunDependencyGraphBuilder(declarationIndex, classSource, fieldToDependencyInfosMap) {
        m ->
        if (!loadAnnotationsForDependency(resultingAnnotationsMap, m, declarationIndexWithDependencies)) {
            errorHandler.warning("Method called but not present in the code: " + m)
        }
        null
    }

    val methodGraph = methodGraphBuilder.build()

    val packageGraphBuilder = PackageDependencyGraphBuilder(methodGraph)
    val packageGraph = packageGraphBuilder.build()

    val nonInterestingNodes = packageGraph.nodes subtract packageGraph.getTransitivelyInterestingNodes { packageIsInteresting(it.data.name) }
    packageGraphBuilder.removeGraphNodes {it in nonInterestingNodes}

    val classHierarchy = buildClassHierarchyGraph(classSource)
    val methodHierarchy = buildMethodHierarchy(classHierarchy)
    packageGraphBuilder.extendWithHierarchy(methodHierarchy)

    methodGraphBuilder.removeGraphNodes { packageGraph.findNode(Package(it.data.packageName)) == null }

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
                declarationIndexWithDependencies,
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

    return propagateAnnotations(inferrers, inferenceResult, propagationOverrides, methodHierarchy)
}

private fun <K: AnalysisType> propagateAnnotations(
        inferrers: Map<K, AnnotationInferrer<Any, Qualifier>>,
        inferenceResult: InferenceResult<K>,
        propagationOverrides: Map<K, Annotations<Any>>,
        methodHierarchy: HierarchyGraph<Method>
): InferenceResult<K> {
    val map = (inferenceResult.groupByKey as MutableMap<K, InferenceResultGroup<Any>>)
    for ((key, group) in map) {
        val propagatedAnnotations = propagateMetadata(
                methodHierarchy,
                inferrers[key]!!.lattice,
                group.inferredAnnotations,
                group.propagatedPositions as MutableSet<AnnotationPosition>,
                propagationOverrides[key]!!
        )
        map[key] = group.copy(inferredAnnotations = propagatedAnnotations)
    }

    return inferenceResult
}

private fun <K: AnalysisType, A> inferAnnotationsOnMutuallyRecursiveMethods(
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

        val t = inferrers.mapValues (
                {(key, inferrer) -> inferrer.getFrameTransformer(annotationsMap[key]!!, declarationIndex) as FrameTransformer<QualifiedValueSet<MultiQualifier<K>>>}
        )

        val analysisResult = methodNodes(method).runQualifierAnalysis<MultiQualifier<K>>(
                method.declaringClass,
                MultiQualifierSet(inferrers.mapValues (
                        {(key, inferrer) -> inferrer.qualifierSet}
                )),
                MultiFrameTransformer<K, QualifiedValueSet<MultiQualifier<K>>>(t),
                MultiQualifierEvaluator(inferrers.mapValues (
                        {(key, inferrer) -> inferrer.getQualifierEvaluator(PositionsForMethod(method), annotationsMap[key]!!, declarationIndex)}
                ))
        )

        for ((key, inferrer) in inferrers) {
            val annotations = annotationsMap[key]!!

            val inferredAnnotations = inferrer.inferAnnotationsFromMethod<MultiQualifier<K>>(
                    method, methodNodes(method), analysisResult, fieldDependencyInfoProvider, declarationIndex, annotations)

            var changed = false
            annotations.copyAllChanged(inferredAnnotations as Annotations<A>) { pos, previous, new ->
                changed = true
                new
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
