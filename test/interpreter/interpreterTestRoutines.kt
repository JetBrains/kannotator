package interpreter

import java.io.File
import java.io.PrintStream
import junit.framework.Assert.*
import kotlinlib.*
import org.jetbrains.kannotator.asm.util.AsmInstructionRenderer
import org.jetbrains.kannotator.controlFlow.*
import org.jetbrains.kannotator.controlFlow.builder.AsmInstructionMetadata
import util.controlFlow.GraphBuilderCallbacks
import org.jetbrains.kannotator.controlFlow.builder.STATE_BEFORE
import util.controlFlow.buildGraphsForAllMethods
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Type
import org.objectweb.asm.commons.Method as AsmMethod
import org.objectweb.asm.tree.AbstractInsnNode
import util.controlFlow.MethodAndGraph
import java.io.FileInputStream
import java.io.Reader
import java.io.FileWriter
import java.io.PrintWriter
import org.jetbrains.kannotator.declarations.ClassName
import util.getClassReader

fun PrintStream.appendStates(instructions: Collection<Instruction>) {
    val renderer = AsmInstructionRenderer()
    for ((i, instruction) in instructions.iterator().indexed) {
        val metadata = instruction.metadata
        if (metadata is LabelMetadata<*>) continue
        val state = instruction[STATE_BEFORE]
        if (state == null) {
            this.println("Unreachable: ${instruction}")
        }
        else {
            this.println("Frame")
            this.println("  Locals")
            for ((i, value) in state.localVariables.indexed) {
                this.println("    locals[$i] = ${value.sorted()}")
            }
            this.println("  Stack")
            for ((i, value) in state.stack.indexed) {
                this.println("    stack[$i] = ${value.sorted()}")
            }
            when (metadata) {
                is AsmInstructionMetadata -> {
                    val insn: AbstractInsnNode = metadata.asmInstruction
                    this.println("Offset $i: ${renderer.render(insn)}")
                }
                else -> throw IllegalArgumentException("Unknown metadata type: ${metadata}")
            }
        }
    }
}

fun writeGraphsToFile(file: File, className: ClassName, methodsAndGraphs: Collection<MethodAndGraph>) {
    val actualStream = PrintStream(file)
    try {
        actualStream.println(className)
        for ((method, graph) in methodsAndGraphs) {
            actualStream.println("")
            actualStream.println(method)
            actualStream.appendStates(graph.build().instructions)
            actualStream.println("==========================================================================================")
            actualStream.println("")
            actualStream.println("")
        }
    }
    finally {
        actualStream.close()
    }
}

fun doTest(theClass: Class<out Any>) {
    val classReader = getClassReader(theClass)
    doTest(File("testData/"), classReader)
}

val KB = 1024
val MB = KB * KB

private val buffer = CharArray(1 * MB)

fun doTest(
        baseDir: File,
        classReader: ClassReader,
        failOnNoData: Boolean = true,
        dumpMethodNames: Boolean = false
) {
    val expectedFile = File(baseDir, classReader.getClassName() + ".txt")
    expectedFile.getParentFile()!!.mkdirs()

    val errorFile = File(expectedFile.getPath().removeSuffix(".txt") + ".errors.txt")
    errorFile.delete()

    val methodsAndGraphs = buildGraphsForAllMethods(classReader, object : GraphBuilderCallbacks() {

        override fun beforeMethod(internalClassName: String, methodName: String, methodDesc: String): Boolean {
            if (dumpMethodNames) println("    " + methodName + methodDesc)
            return true
        }

        override fun error(internalClassName: String, methodName: String, methodDesc: String, e: Throwable) {
            FileWriter(errorFile, true).use {
                w ->
                w.append("===========================================================\n")
                w.append("$internalClassName :: $methodName$methodDesc\n")
                e.printStackTrace(PrintWriter(w))
            }
            System.err.println("===========================================================")
            System.err.println("$internalClassName :: $methodName$methodDesc")
            e.printStackTrace()
        }
    })

    val actualFile = File(expectedFile.getPath().removeSuffix(".txt") + ".actual.txt")
    writeGraphsToFile(actualFile, ClassName.fromInternalName(classReader.getClassName()), methodsAndGraphs)

    if (!expectedFile.exists()) {
        expectedFile.getParentFile()!!.mkdirs()
        actualFile.copyTo(expectedFile)
        val message = "Expected data file file does not exist: ${expectedFile}. It is created from actual data"
        if (failOnNoData) {
            fail(message)
        }
        else {
            System.err.println(message)
        }
    }

    // Some of these files are very big and do not fit into memory,
    // so we break them into chunks and compare chunk-by-chunk
    val actualReader = FileInputStream(actualFile).reader()
    val expectedReader = FileInputStream(expectedFile).reader()

    try {
        while (true)  {
            val chunkActual = actualReader.readWithBuffer(buffer)
            val chunkExpected = expectedReader.readWithBuffer(buffer)

            if (chunkActual == null || chunkExpected == null) break;
            assertEquals(chunkExpected.toUnixSeparators(), chunkActual.toUnixSeparators())
        }
    }
    finally {
        actualReader.close()
        expectedReader.close()
    }

    actualFile.delete()
}

fun Reader.readWithBuffer(buffer: CharArray): String? {
    val charsRead = read(buffer)
    if (charsRead == -1) return null
    return StringBuilder().append(buffer, 0, charsRead).toString()
}

fun Set<Value>.sorted() = map {v -> v.toString()}.toSortedList()