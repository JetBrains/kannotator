package interpreter

import java.io.File
import junit.framework.Assert.*
import kotlinlib.*
import org.jetbrains.kannotator.asm.util.AsmInstructionRenderer
import org.jetbrains.kannotator.controlFlow.*
import org.jetbrains.kannotator.controlFlowBuilder.AsmInstructionMetadata
import org.jetbrains.kannotator.controlFlowBuilder.STATE_BEFORE
import org.jetbrains.kannotator.controlFlowBuilder.buildGraphsForAllMethods
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Type
import org.objectweb.asm.commons.Method as AsmMethod
import org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.kannotator.controlFlowBuilder.GraphBuilderCallbacks

fun StringBuilder.appendStates(instructions: Collection<Instruction>) {
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

fun doTest(theClass: Class<out Any>) {
    val classType = Type.getType(theClass)
    val classReader = ClassReader(theClass.getCanonicalName())
    doTest(File("testData/"), classType, classReader)
}

fun doTest(
        baseDir: File,
        classType: Type,
        classReader: ClassReader,
        failOnNoData: Boolean = true,
        dumpMethodNames: Boolean = false
) {
    val methodsAndGraphs = buildGraphsForAllMethods(classType, classReader, object : GraphBuilderCallbacks() {

        override fun beforeMethod(internalClassName: String, methodName: String, methodDesc: String): Boolean {
            if (dumpMethodNames) println("    " + methodName + methodDesc)
            return true
        }

        override fun error(internalClassName: String, methodName: String, methodDesc: String, e: Throwable) {
            System.err.println("===========================================================")
            System.err.println("$internalClassName :: $methodName$methodDesc")
            e.printStackTrace()
        }
    })

    val actual = buildString {
        sb ->
        sb.println(classType.getInternalName())

        for ((method, graph) in methodsAndGraphs) {
            sb.println("")
            sb.println(method)
            sb.appendStates(graph.build().instructions)
            sb.println("==========================================================================================")
            sb.println("")
            sb.println("")
        }
    }

    val expectedFile = File(baseDir, classType.getInternalName() + ".txt")
    if (!expectedFile.exists()) {
        expectedFile.getParentFile()!!.mkdirs()
        expectedFile.writeText(actual)
        if (failOnNoData) {
            fail("Expected data file file does not exist: ${expectedFile}. It is created from actual data")
        }
    }
    val expected = expectedFile.readText()

    assertEquals(expected, actual)
}

fun Set<Value>.sorted() = map {v -> v.toString()}.toSortedList()