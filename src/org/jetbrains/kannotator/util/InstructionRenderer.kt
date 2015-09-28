package org.jetbrains.kannotator.asm.util

import org.objectweb.asm.Label
import java.util.HashMap
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.util.Textifier
import org.objectweb.asm.util.TraceMethodVisitor
import kotlinlib.*
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.LineNumberNode
import org.objectweb.asm.tree.JumpInsnNode
import org.objectweb.asm.util.Printer

public class AsmInstructionRenderer {
    private val labelNames: MutableMap<Label, String> = HashMap()

    public fun labelName(label: Label): String {
        return labelNames.getOrPut(label) {"L" + labelNames.size()}
    }

    public fun render(insn: AbstractInsnNode): String {
        if (insn is LabelNode) {
            return labelName(insn.label)
        }
        if (insn is LineNumberNode) {
            return "LINENUMBER ${insn.line} ${labelName(insn.start.label)}"
        }
        if (insn is JumpInsnNode) {
            return Printer.OPCODES[insn.opcode] + " ${labelName(insn.label.label)}"
        }
        val textifier = Textifier()
        insn.accept(TraceMethodVisitor(textifier))
        return textifier.getText()!!.map {x -> x.toString().trim()}.join(" ")
    }
}