package inference

import junit.framework.TestCase
import junit.framework.Assert.*
import java.io.File
import java.util.Collections
import java.io.PrintStream
import java.io.FileOutputStream
import kotlinlib.*
import org.jetbrains.kannotator.annotations.io.toAnnotationKey
import org.jetbrains.kannotator.main.ProgressMonitor
import org.jetbrains.kannotator.declarations.Method
import java.io.FileInputStream
import interpreter.readWithBuffer
import java.util.TreeMap
import org.jetbrains.kannotator.annotationsInference.Annotation
import org.jetbrains.kannotator.annotationsInference.nullability.NullabilityAnnotation
import java.util.ArrayList
import util.assertEqualsOrCreate
import org.jetbrains.kannotator.declarations.AnnotationsImpl
import org.jetbrains.kannotator.declarations.AnnotationPosition
import kotlin.test.assertTrue
import org.jetbrains.kannotator.declarations.Annotations
import org.jetbrains.kannotator.main.*
import util.findJarsInLibFolder
import org.jetbrains.kannotator.index.FileBasedClassSource
import org.junit.Assert
import util.*
import java.util.HashSet
import org.jetbrains.kannotator.annotations.io.writeAnnotations
import java.io.FileWriter
import org.jetbrains.kannotator.declarations.PositionsForMethod
import org.jetbrains.kannotator.annotations.io.AnnotationDataImpl
import org.jetbrains.kannotator.kotlinSignatures.renderMethodSignature
import org.jetbrains.kannotator.annotationsInference.mutability.MutabilityAnnotation
import org.jetbrains.kannotator.kotlinSignatures.kotlinSignatureToAnnotationData
import java.io.StringWriter
import org.jetbrains.kannotator.index.AnnotationKeyIndex
import org.jetbrains.kannotator.declarations.MutableAnnotations
import org.jetbrains.kannotator.index.DeclarationIndex
import kotlinSignatures.KotlinSignatureTestData.MutabilityNoAnnotations
import java.io.BufferedReader
import java.io.FileReader
import org.jetbrains.kannotator.annotations.io.AnnotationData
import org.jetbrains.kannotator.declarations.isPublicOrProtected
import org.jetbrains.kannotator.declarations.isPublic
import org.jetbrains.kannotator.annotations.io.parseAnnotations
import java.util.HashMap
import org.jetbrains.kannotator.declarations.forEachValidPosition
import org.jetbrains.kannotator.annotationsInference.nullability.*
import kotlin.dom.addClass
import java.util.LinkedHashMap
import org.jetbrains.kannotator.classHierarchy.*
import org.jetbrains.kannotator.declarations.ClassMember
import org.jetbrains.kannotator.declarations.getInternalPackageName
import org.jetbrains.kannotator.funDependecy.buildFunctionDependencyGraph
import org.jetbrains.kannotator.funDependecy.getTopologicallySortedStronglyConnectedComponents
import org.jetbrains.kannotator.annotations.io.methodsToAnnotationsMap
import org.jetbrains.kannotator.annotations.io.getPackageName

class MethodHierarchyTest: TestCase() {
    private fun doMethodHierarchyTest(testedJarSubstring: String) {
        val jars = findJarsInLibFolder().filter { f -> f.getName().contains(testedJarSubstring) }
        Assert.assertEquals("Test failed to find exactly one jar file with request '$testedJarSubstring'", jars.size, 1);

        val annotationFiles = ArrayList<File>()
        File("lib").recurseFiltered({ f -> f.isFile() && f.getName().endsWith(".xml") }, { f -> annotationFiles.add(f) })

        val jar = jars.first()
        println("start: $jar")

        val classHierarchy = buildClassHierarchyGraph(FileBasedClassSource(arrayList(jar)))
        val methodHierarchy = buildMethodHierarchy(classHierarchy)

        val expectedFile = File("testData/inferenceData/integrated/methodHierarchy/${jar.getName()}.txt")
        val outFile = File(expectedFile.getPath().removeSuffix(".txt") + ".actual.txt")
        outFile.getParentFile()!!.mkdirs()

        val methodNodes = ArrayList<HierarchyNode<Method>>()
        for (methodNode in methodHierarchy.nodes) {
            methodNodes.add(methodNode)
        }
        val sortedMethodNodes = methodNodes.sortBy {it.data.toString()}

        PrintStream(FileOutputStream(outFile)) use {
            p ->
            for (methodNode in sortedMethodNodes) {
                p.println(methodNode.data)
                val parentNodes = methodNode.parentNodes().sortByToString()
                val childNodes = methodNode.childNodes().sortByToString()

                if (!parentNodes.isEmpty()) {
                    p.println("\tParents: ")
                    for (node in parentNodes) {
                        p.println("\t\t${node.data}")
                    }
                }

                if (!childNodes.isEmpty()) {
                    p.println("\tChildren: ")
                    for (node in childNodes) {
                        p.println("\t\t${node.data}")
                    }
                }
            }
        }

        assertEqualsOrCreate(expectedFile, outFile.readText(), false)

        outFile.delete()
    }

    fun testJDK1_7_0_09_rt_jar_Methods() = doMethodHierarchyTest("jdk_1_7_0_09_rt.jar")
}