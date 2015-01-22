package org.jetbrains.kannotator.graphs.dependencyGraphs

import java.util.ArrayList
import java.util.HashMap
import kotlinlib.flags
import org.jetbrains.kannotator.index.ClassSource
import org.jetbrains.kannotator.index.DeclarationIndex
import org.jetbrains.kannotator.index.FieldDependencyInfo
import org.jetbrains.kannotator.index.buildFieldsDependencyInfos
import org.objectweb.asm.ClassReader.*
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.ASM4
import org.objectweb.asm.commons.Method as AsmMethod
import org.objectweb.asm.Type
import org.jetbrains.kannotator.graphs.NodeImpl
import org.jetbrains.kannotator.graphs.GraphBuilder
import org.jetbrains.kannotator.graphs.DefaultNodeImpl
import org.jetbrains.kannotator.graphs.GraphImpl
import org.jetbrains.kannotator.declarations.*
import org.jetbrains.kannotator.graphs.Graph
import org.jetbrains.kannotator.classHierarchy.HierarchyGraph
import org.jetbrains.kannotator.declarations.Package

public fun buildPackageDependencyGraph(
        funDependencyGraph: Graph<Method, *>
) : Graph<Package, Nothing?> = PackageDependencyGraphBuilder(funDependencyGraph).build()

public class PackageDependencyGraphBuilder(
        private val funDependencyGraph: Graph<Method, *>
): GraphBuilder<Package, Package, Nothing?, GraphImpl<Package, Nothing?>>(true, true) {
    override fun newGraph(): GraphImpl<Package, Nothing?> = GraphImpl(createNodeMap)
    override fun newNode(data: Package): NodeImpl<Package, Nothing?> = DefaultNodeImpl(data)

    public fun build(): Graph<Package, Nothing?> {
        for (nodeFrom in funDependencyGraph.nodes) {
            val packageFrom = getOrCreateNode(Package(nodeFrom.data.packageName))
            for (edge in nodeFrom.outgoingEdges) {
                val packageTo = getOrCreateNode(Package(edge.to.data.packageName))
                getOrCreateEdge(null, packageFrom, packageTo)
            }
        }

        return toGraph()
    }

    public fun extendWithHierarchy(funHierarchyGraph: HierarchyGraph<Method>): Graph<Package, Nothing?> {
        for (nodeFrom in funHierarchyGraph.hierarchyNodes) {
            val packageFrom = getOrCreateNode(Package(nodeFrom.data.packageName))
            for (edge in nodeFrom.outgoingEdges) {
                val packageTo = getOrCreateNode(Package(edge.to.data.packageName))
                getOrCreateEdge(null, packageFrom, packageTo)
                getOrCreateEdge(null, packageTo, packageFrom)
            }
        }

        return toGraph()
    }
}


