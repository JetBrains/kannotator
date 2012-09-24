package org.jetbrains.kannotator.controlFlowBuilder

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Label
import org.objectweb.asm.util.Printer
import org.objectweb.asm.tree.analysis.BasicInterpreter
import org.objectweb.asm.tree.analysis.Analyzer
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.Type
import org.objectweb.asm.Type.*
import org.objectweb.asm.tree.ClassNode

import kotlinlib.*
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.analysis.BasicValue
import org.objectweb.asm.tree.analysis.Interpreter
import org.objectweb.asm.tree.TryCatchBlockNode
import org.jetbrains.kannotator.controlFlow.ControlFlowGraphBuilder
import org.jetbrains.kannotator.controlFlow.InstructionMetadata
import java.util.HashMap
import org.jetbrains.kannotator.controlFlow.Instruction
import org.jetbrains.kannotator.controlFlow.ControlFlowGraph
import java.util.ArrayList
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.LineNumberNode
import org.objectweb.asm.tree.FrameNode
import org.objectweb.asm.tree.analysis.Value
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.Handle
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.TypeInsnNode
import org.objectweb.asm.tree.IntInsnNode
import org.objectweb.asm.tree.MultiANewArrayInsnNode
import org.objectweb.asm.tree.InvokeDynamicInsnNode
import org.objectweb.asm.tree.MethodInsnNode

public fun buildControlFlowGraph(classReader: ClassReader, methodName: String, methodDesc: String): ControlFlowGraph {
    val classVisitor = GraphBuilderClassVisitor(classReader.getClassName()!!, methodName, methodDesc)
    classReader.accept(classVisitor, 0)
    return classVisitor.graph
}

private class GraphBuilderClassVisitor(val className: String, val methodName: String, val methodDesc: String) : ClassVisitor(ASM4) {

    private val graphBuilder = ControlFlowGraphBuilder<Label>()

    val graph: ControlFlowGraph
        get() = graphBuilder.build()

    public override fun visitMethod(access: Int, name: String, desc: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
        val mv = super.visitMethod(access, name, desc, signature, exceptions)
        println(name + desc)
        if (name != methodName || desc != methodDesc) {
            return mv
        }
        val methodNode = MethodNode(access, name, desc, signature, exceptions)
        return GraphBuilderMethodVisitor(className, graphBuilder, methodNode)
    }
}

private class GraphBuilderMethodVisitor(
        val owner: String,
        val graphBuilder: ControlFlowGraphBuilder<Label>,
        val methodNode: MethodNode
) : MethodVisitor(ASM4, methodNode) {

    public override fun visitEnd() {
        super.visitEnd()
        val analyzer = GraphBuilderAnalyzer(graphBuilder, methodNode)
        analyzer.analyze(owner, methodNode)
        val frames = analyzer.getFrames()
        val instructions = methodNode.instructions
        for ((i, frame) in frames.indexed) {
            if (frame == null) {
                println("Unreachable: ${instructions[i]}")
            }
            else {
                println(instructions[i])
            }
        }
    }
}


private class GraphBuilderAnalyzer(val graph: ControlFlowGraphBuilder<Label>, val methodNode: MethodNode) : Analyzer<AsmPossibleValues>(GraphBuilderInterpreter()) {
    private val instructions = methodNode.instructions.iterator().map { it -> it.toInstruction() }.toArrayList()

    fun AbstractInsnNode.toInstruction(): Instruction {
        if (this is LabelNode) {
            return graph.getLabelInstruction(this.getLabel())
        }
        return graph.newInstruction(Metadata(this))
    }


    {
        graph.setEntryPoint(instructions[0])
    }


    private class Metadata(val asmInstruction: AbstractInsnNode) : InstructionMetadata {
        public fun toString(): String {
            val opcode = asmInstruction.getOpcode()
            if (opcode == -1) {
                return when (asmInstruction) {
                    is LineNumberNode -> "Line num: ${asmInstruction.line}"
                    is FrameNode -> "frame node"
                    else -> asmInstruction.toString()
                }
            }
            return Printer.OPCODES[opcode]!!
        }

    }

    protected override fun newControlFlowExceptionEdge(insn: Int, successor: Int): Boolean {
        createEdge(insn, successor)
        return super<Analyzer>.newControlFlowExceptionEdge(insn, successor)
    }


    protected override fun newControlFlowEdge(insn: Int, successor: Int) {
        createEdge(insn, successor)
        super<Analyzer>.newControlFlowEdge(insn, successor)
    }

    private fun createEdge(from: Int, to: Int) {
        graph.addEdge(instructions[from], instructions[to])
    }

}

private enum class Size(val size: Int) {
    ONE : Size(1)
    TWO : Size(2)
}

private class AsmPossibleValues(val _type: Type?, val createdAtInsn: AbstractInsnNode? = null) : Value {
    public override fun getSize(): Int = _type?.getSize() ?: 1
}

private class GraphBuilderInterpreter: Interpreter<AsmPossibleValues>(ASM4) {
    public override fun newValue(_type: Type?): AsmPossibleValues? {
        if (_type?.getSort() == Type.VOID)
            return null
        return AsmPossibleValues(_type)
    }

    private fun newValueAtInstruction(_type: Type, insn: AbstractInsnNode): AsmPossibleValues? {
        if (_type.getSort() == Type.VOID)
            return null
        return AsmPossibleValues(_type, insn)
    }

    public override fun newOperation(insn: AbstractInsnNode): AsmPossibleValues? {
        return when (insn.getOpcode()) {
            ACONST_NULL -> newValueAtInstruction(Type.getType("null"), insn)
            ICONST_M1, ICONST_0, ICONST_1, ICONST_2, ICONST_3, ICONST_4, ICONST_5 -> newValueAtInstruction(INT_TYPE, insn)
            LCONST_0, LCONST_1 -> newValueAtInstruction(LONG_TYPE, insn)
            FCONST_0, FCONST_1, FCONST_2 -> newValueAtInstruction(FLOAT_TYPE, insn)
            DCONST_0, DCONST_1 -> newValueAtInstruction(DOUBLE_TYPE, insn)
            BIPUSH, SIPUSH -> newValueAtInstruction(INT_TYPE, insn)
            LDC -> {
                val ldc = insn as LdcInsnNode
                val cst = ldc.cst
                fun illegalLdcConstant(): Throwable {
                    return IllegalArgumentException("Illegal LCD constant: $cst : ${cst.javaClass}")
                }
                when (cst) {
                    is Int -> newValueAtInstruction(INT_TYPE, insn)
                    is Long -> newValueAtInstruction(LONG_TYPE, insn)
                    is Float -> newValueAtInstruction(FLOAT_TYPE, insn)
                    is Double -> newValueAtInstruction(DOUBLE_TYPE, insn)
                    is String -> newValueAtInstruction(Type.getObjectType("java/lang/String"), insn)
                    is Type -> {
                        when (cst.getSort()) {
                            OBJECT, ARRAY -> newValueAtInstruction(Type.getObjectType("java/lang/Class"), insn)
                            METHOD -> newValueAtInstruction(Type.getObjectType("java/lang/invoke/MethodType"), insn)
                            else -> throw illegalLdcConstant()
                        }
                    }
                    is Handle -> newValueAtInstruction(Type.getObjectType("java/lang/invoke/MethodHandle"), insn)
                    else -> throw illegalLdcConstant()
                }

            }
            JSR -> newValueAtInstruction(VOID_TYPE, insn);
            GETSTATIC -> newValueAtInstruction(Type.getType((insn as FieldInsnNode).desc), insn)
            NEW -> newValueAtInstruction(Type.getObjectType((insn as TypeInsnNode).desc), insn)
            else -> throw unsupportedInstruction(insn)
        }
    }

    public override fun copyOperation(insn: AbstractInsnNode, value: AsmPossibleValues): AsmPossibleValues? {
        return value
    }

    public override fun unaryOperation(insn: AbstractInsnNode, value: AsmPossibleValues): AsmPossibleValues? {
        return when (insn.getOpcode()) {
            INEG, IINC, L2I, F2I, D2I, I2B, I2C, I2S -> newValueAtInstruction(INT_TYPE, insn)
            FNEG, I2F, L2F, D2F -> newValueAtInstruction(FLOAT_TYPE, insn)
            LNEG, I2L, F2L, D2L -> newValueAtInstruction(LONG_TYPE, insn)
            DNEG, I2D, L2D, F2D -> newValueAtInstruction(DOUBLE_TYPE, insn)
            IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE, TABLESWITCH,
            LOOKUPSWITCH, IRETURN, LRETURN, FRETURN, DRETURN, ARETURN, PUTSTATIC -> null
            GETFIELD -> newValueAtInstruction(Type.getType((insn as FieldInsnNode).desc), insn)
            NEWARRAY -> newValueAtInstruction(arrayType((insn as IntInsnNode).operand), insn)
            ANEWARRAY -> newValueAtInstruction(Type.getType("[" + Type.getObjectType((insn as TypeInsnNode).desc)), insn)
            ARRAYLENGTH -> newValueAtInstruction(INT_TYPE, insn)
            ATHROW -> null
            CHECKCAST -> newValueAtInstruction(Type.getObjectType((insn as TypeInsnNode).desc), insn)
            INSTANCEOF -> newValueAtInstruction(INT_TYPE, insn)
            MONITORENTER, MONITOREXIT, IFNULL, IFNONNULL -> null
            else -> throw unsupportedInstruction(insn)
        }
    }

    private fun unsupportedInstruction(insn: AbstractInsnNode): Throwable {
        return IllegalArgumentException("Unsupported instruction: " + Printer.OPCODES[insn.getOpcode()])
    }

    private fun arrayType(operand: Int): Type {
        return when (operand) {
            T_BOOLEAN -> Type.getType("[Z")
            T_CHAR -> Type.getType("[C")
            T_BYTE -> Type.getType("[B")
            T_SHORT -> Type.getType("[S")
            T_INT -> Type.getType("[I")
            T_FLOAT -> Type.getType("[F")
            T_DOUBLE -> Type.getType("[D")
            T_LONG -> Type.getType("[J")
            else -> throw IllegalArgumentException("Incorrect array type: $operand")
        }
    }

    public override fun binaryOperation(
            insn: AbstractInsnNode,
            value1: AsmPossibleValues,
            value2: AsmPossibleValues
    ): AsmPossibleValues? {
        return when (insn.getOpcode()) {
            IALOAD, BALOAD, CALOAD, SALOAD, IADD, ISUB, IMUL,
            IDIV, IREM, ISHL, ISHR, IUSHR, IAND, IOR, IXOR -> newValueAtInstruction(INT_TYPE, insn)
            FALOAD, FADD, FSUB, FMUL, FDIV, FREM -> newValueAtInstruction(FLOAT_TYPE, insn)
            LALOAD, LADD, LSUB, LMUL, LDIV, LREM, LSHL, LSHR, LUSHR, LAND, LOR, LXOR -> newValueAtInstruction(LONG_TYPE, insn)
            DALOAD, DADD, DSUB, DMUL, DDIV, DREM -> newValueAtInstruction(DOUBLE_TYPE, insn)
            AALOAD -> newValueAtInstruction(Type.getObjectType("java/lang/Object"), insn)
            LCMP, FCMPL, FCMPG, DCMPL, DCMPG -> newValueAtInstruction(INT_TYPE, insn)
            IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE, IF_ACMPEQ, IF_ACMPNE, PUTFIELD -> null
            else -> throw unsupportedInstruction(insn)
        }
    }

    public override fun ternaryOperation(
            insn: AbstractInsnNode,
            value1: AsmPossibleValues,
            value2: AsmPossibleValues,
            value3: AsmPossibleValues
    ): AsmPossibleValues? {
        return null
    }

    public override fun naryOperation(insn: AbstractInsnNode, values: List<out AsmPossibleValues>): AsmPossibleValues? {
        return when (insn) {
            is MultiANewArrayInsnNode -> newValueAtInstruction(Type.getType(insn.desc), insn)
            is InvokeDynamicInsnNode -> newValueAtInstruction(Type.getReturnType(insn.desc), insn)
            is MethodInsnNode -> newValueAtInstruction(Type.getReturnType(insn.desc), insn)
            else -> throw unsupportedInstruction(insn)
        }
    }

    public override fun returnOperation(insn: AbstractInsnNode, value: AsmPossibleValues, expected: AsmPossibleValues) {
    }

    public override fun merge(v: AsmPossibleValues, w: AsmPossibleValues): AsmPossibleValues {
        if (v != w) {
             newValue(null)
        }
        return v
    }

}
