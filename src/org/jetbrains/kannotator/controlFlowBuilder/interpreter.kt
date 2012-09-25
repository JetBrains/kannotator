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
import org.objectweb.asm.tree.analysis.Value as AsmValue
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.Handle
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.TypeInsnNode
import org.objectweb.asm.tree.IntInsnNode
import org.objectweb.asm.tree.MultiANewArrayInsnNode
import org.objectweb.asm.tree.InvokeDynamicInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.jetbrains.kannotator.asm.util.toOpcodeString
import org.objectweb.asm.tree.analysis.Frame
import org.objectweb.asm.tree.InsnList
import org.jetbrains.kannotator.controlFlow.Value
import com.gs.collections.impl.set.strategy.mutable.UnifiedSetWithHashingStrategy
import com.gs.collections.api.block.HashingStrategy
import kotlin.nullable.hashCodeOrDefault
import java.util.Collections

private class TypedValue(val id: Int, val _type: Type?, val interesting: Boolean, val createdAtInsn: AbstractInsnNode? = null) : Value {
    public fun getSize(): Int = when (_type) {
        null -> 1
        PRIMITIVE_TYPE_SIZE_2 -> 2
        else -> _type.getSize()
    }

    public fun toString(): String {
        val typeAndId = "$_type#$id"
        return (if (interesting) "!" else "") + typeAndId
    }
}

val PRIMITIVE_TYPE_SIZE_1 = Type.getType("P1")
val PRIMITIVE_TYPE_SIZE_2 = Type.getType("P2")
val PRIMITIVE_VALUE_SIZE_1 = TypedValue(-1, PRIMITIVE_TYPE_SIZE_1, false, null)
val PRIMITIVE_VALUE_SIZE_2 = TypedValue(-1, PRIMITIVE_TYPE_SIZE_2, false, null)
val NULL_TYPE = Type.getType("null")
val NULL_VALUE = TypedValue(-1, NULL_TYPE, false, null)

val PRIMITIVE_1_AS_SET = AsmPossibleValues(PRIMITIVE_VALUE_SIZE_1)
val PRIMITIVE_2_AS_SET = AsmPossibleValues(PRIMITIVE_VALUE_SIZE_2)
val NULL_AS_SET = AsmPossibleValues(NULL_VALUE)
val EMPTY_VALUES = AsmPossibleValues()

fun AsmPossibleValues(value: TypedValue) = PossibleTypedValues(value.getSize(), hashSet(value))

fun AsmPossibleValues(vararg values: TypedValue): PossibleTypedValues {
    if (values.isEmpty()) {
        return PossibleTypedValues(1, hashSet())
    }
    val size = values[0].getSize()
    for (value in values) {
        if (value.getSize() != size) throw IllegalStateException("Inconsistent sizes: ${values.toList()}")
    }
    return PossibleTypedValues(size, values.toSet())
}

private class PossibleTypedValues(val _size: Int, val values: Set<TypedValue>) : AsmValue {
    public override fun getSize(): Int = _size

    public override fun toString(): String {
        return values.toString()
    }
}

fun PossibleTypedValues.merge(other: PossibleTypedValues): PossibleTypedValues {
    assert(getSize() == other.getSize()) {"Sizes don't match"}

    if (values.isEmpty()) return other
    if (other.values.isEmpty()) return this

    val mergedSet = UnifiedSetWithHashingStrategy(object : HashingStrategy<TypedValue> {
        public override fun equals(object1: TypedValue?, object2: TypedValue?): Boolean {
            if (object1 identityEquals object2) return true
            if (object1 == null || object2 == null) return false

            // Interesting values don't merge
            if (object1.interesting || object2.interesting) return false

            return object1.createdAtInsn identityEquals object2.createdAtInsn
        }

        public override fun computeHashCode(_object: TypedValue?): Int {
            if (_object == null) return 0
            val r1 = if (_object.interesting) 13 else 17
            val r2 = _object.createdAtInsn.hashCodeOrDefault(0)
            return r1 * r2
        }
    })

    mergedSet.addAll(values)
    mergedSet.addAll(other.values)

    if (mergedSet == values) return this

    return PossibleTypedValues(getSize(), mergedSet)
}

enum class MethodKind {
    INSTANCE
    STATIC
}

fun MethodKind.hasThis() = this == MethodKind.INSTANCE

private class GraphBuilderInterpreter(val methodKind: MethodKind, val desc: String): Interpreter<PossibleTypedValues>(ASM4) {
    private val valuesByInstructionCache: MutableMap<AbstractInsnNode, PossibleTypedValues> = HashMap()
    private var valueSetsCreated: Int = 0
    private var valuesCreated: Int = 0

    private fun createValue(_type: Type, interesting: Boolean, insn: AbstractInsnNode?): TypedValue {
        return TypedValue(valuesCreated++, _type, interesting, insn)
    }

    private fun specialValue(_type: Type): PossibleTypedValues? {
        return when (_type) {
            NULL_TYPE -> NULL_AS_SET
            BYTE_TYPE, SHORT_TYPE, INT_TYPE,
            FLOAT_TYPE,
            CHAR_TYPE,
            BOOLEAN_TYPE -> PRIMITIVE_1_AS_SET
            LONG_TYPE, DOUBLE_TYPE -> PRIMITIVE_2_AS_SET
            else -> null
        }
    }

    public override fun newValue(_type: Type?): PossibleTypedValues? {
        if (_type?.getSort() == Type.VOID)
            return null

        if (_type == null) return EMPTY_VALUES

        if (specialValue(_type) != null) return specialValue(_type)

        val skip = (if (methodKind.hasThis()) 1 else 0) + (if (Type.getReturnType(desc) == VOID_TYPE) 0 else 1)
        val interesting = valueSetsCreated in skip..Type.getArgumentTypes(desc).size + skip // 0 is for return type
        valueSetsCreated++

        return AsmPossibleValues(createValue(_type, interesting, null))
    }

    private fun newValueAtInstruction(_type: Type, insn: AbstractInsnNode): PossibleTypedValues? {
        if (_type.getSort() == Type.VOID)
            return null

        if (specialValue(_type) != null) return specialValue(_type)

        return valuesByInstructionCache.getOrPut(insn) {
            AsmPossibleValues(createValue(_type, false, insn))
        }
    }

    public override fun newOperation(insn: AbstractInsnNode): PossibleTypedValues? {
        return when (insn.getOpcode()) {
            ACONST_NULL -> newValueAtInstruction(NULL_TYPE, insn)
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

    public override fun copyOperation(insn: AbstractInsnNode, value: PossibleTypedValues): PossibleTypedValues? {
        return value
    }

    public override fun unaryOperation(insn: AbstractInsnNode, value: PossibleTypedValues): PossibleTypedValues? {
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
            CHECKCAST -> value
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
            value1: PossibleTypedValues,
            value2: PossibleTypedValues
    ): PossibleTypedValues? {
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
            value1: PossibleTypedValues,
            value2: PossibleTypedValues,
            value3: PossibleTypedValues
    ): PossibleTypedValues? {
        return null
    }

    public override fun naryOperation(insn: AbstractInsnNode, values: List<out PossibleTypedValues>): PossibleTypedValues? {
        return when (insn) {
            is MultiANewArrayInsnNode -> newValueAtInstruction(Type.getType(insn.desc), insn)
            is InvokeDynamicInsnNode -> newValueAtInstruction(Type.getReturnType(insn.desc), insn)
            is MethodInsnNode -> newValueAtInstruction(Type.getReturnType(insn.desc), insn)
            else -> throw unsupportedInstruction(insn)
        }
    }

    public override fun returnOperation(insn: AbstractInsnNode, value: PossibleTypedValues, expected: PossibleTypedValues) {
    }

    public override fun merge(v: PossibleTypedValues, w: PossibleTypedValues): PossibleTypedValues {
        return v merge w
    }

}

