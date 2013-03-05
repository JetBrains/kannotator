package org.jetbrains.kannotator.graphs.dependencyGraphs

import java.util.ArrayList
import java.util.HashMap
import kotlinlib.flags
import org.jetbrains.kannotator.declarations.ClassName
import org.jetbrains.kannotator.declarations.Field
import org.jetbrains.kannotator.declarations.Method
import org.jetbrains.kannotator.declarations.Package
import org.jetbrains.kannotator.index.ClassSource
import org.jetbrains.kannotator.index.DeclarationIndex
import org.jetbrains.kannotator.index.FieldDependencyInfo
import org.jetbrains.kannotator.index.buildFieldsDependencyInfos
import org.objectweb.asm.ClassReader.*
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.ASM4
import org.objectweb.asm.commons.Method as AsmMethod
import org.jetbrains.kannotator.declarations.getType
import org.objectweb.asm.Type
import org.jetbrains.kannotator.graphs.NodeImpl
import org.jetbrains.kannotator.graphs.GraphBuilder
import org.jetbrains.kannotator.graphs.DefaultNodeImpl
import org.jetbrains.kannotator.graphs.GraphImpl
import org.jetbrains.kannotator.funDependecy.DependencyGraph
import org.jetbrains.kannotator.funDependecy.DependencyGraphImpl
import org.jetbrains.kannotator.declarations.packageName

public fun buildPackageDependencyGraph(funDependencyGraph: DependencyGraph<Method, *>) : DependencyGraph<Package, Nothing?> =
       PackageDependencyGraphBuilder(funDependencyGraph).build()

private class PackageDependencyGraphBuilder(
        private val funDependencyGraph: DependencyGraph<Method, *>
): GraphBuilder<Package, Package, Nothing?, DependencyGraphImpl<Package, Nothing?>>(false, true) {
    override fun newGraph(): DependencyGraphImpl<Package, Nothing?> = DependencyGraphImpl(false)
    override fun newNode(data: Package): NodeImpl<Package, Nothing?> = DefaultNodeImpl(data)

    public fun build(): DependencyGraph<Package, Nothing?> {
        for (nodeFrom in funDependencyGraph.nodes) {
            val packageFrom = getOrCreateNode(Package(nodeFrom.data.declaringClass.packageName))
            for (edge in nodeFrom.outgoingEdges) {
                val packageTo = getOrCreateNode(Package(edge.to.data.declaringClass.packageName))
                getOrCreateEdge(null, packageFrom, packageTo)
            }
        }

        return toGraph()
    }
}
