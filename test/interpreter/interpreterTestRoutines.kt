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
                this.println("    locals[$i] = $value")
            }
            this.println("  Stack")
            for ((i, value) in state.stack.indexed) {
                this.println("    stack[$i] = $value")
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
    val methodsAndGraphs = buildGraphsForAllMethods(classType, ClassReader(theClass.getCanonicalName()))

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

    val expectedFile = File("testData/" + classType.getInternalName() + ".txt")
    if (!expectedFile.exists()) {
        expectedFile.writeText(actual)
        fail("Expected data file file does not exist: ${expectedFile}. It is created from actual data")
    }
    val expected = expectedFile.readText()

    assertEquals(expected, actual)
}