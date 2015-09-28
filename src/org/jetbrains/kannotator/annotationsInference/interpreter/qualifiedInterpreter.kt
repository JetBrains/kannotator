package org.jetbrains.kannotator.controlFlow.builder.analysis

import org.jetbrains.kannotator.declarations.*

import org.objectweb.asm.*
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type.*
import org.objectweb.asm.tree.*
import org.objectweb.asm.tree.analysis.*
import org.objectweb.asm.util.*

import java.util.*
import com.gs.collections.api.block.HashingStrategy
import com.gs.collections.impl.set.strategy.mutable.UnifiedSetWithHashingStrategy

val EMPTY_VALUE_SET = qualifiedValueSetOf<Nothing>()

object TypedValueMergeHashingStrategy : HashingStrategy<QualifiedValue<*>> {
    public override fun equals(object1: QualifiedValue<*>?, object2: QualifiedValue<*>?): Boolean {
        if (object1 identityEquals object2) return true
        if (object1 == null || object2 == null) return false

        // Interesting values don't merge
        if (object1.base.interesting || object2.base.interesting) return false

        // Values computed at different instructions don't merge
        if (object1.base.createdAt != object2.base.createdAt) return false

        // Values with equal qualifiers are merged
        return object1.qualifier == object2.qualifier
    }

    public override fun computeHashCode(_object: QualifiedValue<*>?): Int {
        if (_object == null) return 0

        var r = if (_object.base.interesting) 13 else 17
        r = 19*r + (_object.base.createdAt?.hashCode() ?: 0)
        r = 19*r + _object.qualifier.hashCode()
        return r
    }
}

public class QualifiedValuesInterpreter<Q: Qualifier>(
        val method: Method,
        val qualifierSet: QualifierSet<Q>,
        val qualifierEvaluator: QualifierEvaluator<Q>
): Interpreter<QualifiedValueSet<Q>>(ASM4) {
    val UNDEFINED_VALUE = QualifiedValue(TypedValue(-1, UNDEFINED_TYPE, null, null), qualifierSet.initial)
    val PRIMITIVE_VALUE_SIZE_1 = QualifiedValue(TypedValue(-1, PRIMITIVE_TYPE_SIZE_1, null, null), qualifierSet.initial)
    val PRIMITIVE_VALUE_SIZE_2 = QualifiedValue(TypedValue(-1, PRIMITIVE_TYPE_SIZE_2, null, null), qualifierSet.initial)
    val RETURN_ADDRESS_VALUE = QualifiedValue(TypedValue(-1, Type.VOID_TYPE, null, null), qualifierSet.initial)

    val UNDEFINED_AS_SET = qualifiedValueSetOf(UNDEFINED_VALUE)
    val PRIMITIVE_1_AS_SET = qualifiedValueSetOf(PRIMITIVE_VALUE_SIZE_1)
    val PRIMITIVE_2_AS_SET = qualifiedValueSetOf(PRIMITIVE_VALUE_SIZE_2)
    val RETURN_ADDRESS_VALUE_AS_SET = qualifiedValueSetOf(RETURN_ADDRESS_VALUE)

    private val valuesByInstructionCache: MutableMap<AbstractInsnNode, QualifiedValueSet<Q>> = HashMap()
    private var valueSetsCreated: Int = 0
    private var valuesCreated: Int = 0

    private fun specialValue(_type: Type): QualifiedValueSet<Q>? {
        return when (_type) {
            BYTE_TYPE, SHORT_TYPE, INT_TYPE, FLOAT_TYPE, CHAR_TYPE, BOOLEAN_TYPE -> PRIMITIVE_1_AS_SET
            LONG_TYPE, DOUBLE_TYPE -> PRIMITIVE_2_AS_SET
            else -> null
        }
    }

    private fun createValue(_type: Type, parameterIndex: Int?, insn: AbstractInsnNode?): QualifiedValue<Q> {
        val baseValue = TypedValue(valuesCreated++, _type, parameterIndex, insn)
        val qualifier = qualifierEvaluator.evaluateQualifier(baseValue)
        return QualifiedValue<Q>(baseValue, qualifier)
    }

    private fun valueAtInstruction(_type: Type, insn: AbstractInsnNode): QualifiedValueSet<Q> {
        if (_type.sort == Type.VOID)
            return UNDEFINED_AS_SET

        if (insn.opcode != INSTANCEOF) {
            val sp = specialValue(_type)
            if (sp != null) return sp
        }

        return valuesByInstructionCache.getOrPut(insn) {
            qualifiedValueSetOf(createValue(_type, null, insn))
        }
    }

    private fun unsupportedInstruction(insn: AbstractInsnNode): Throwable {
        return IllegalArgumentException("Unsupported instruction: " + Printer.OPCODES[insn.opcode])
    }

    private fun primitiveArrayType(operand: Int): Type {
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

    fun QualifierSet<Q>.merge(
            vs1: QualifiedValueSet<Q>,
            vs2: QualifiedValueSet<Q>
    ): QualifiedValueSet<Q> {
        var otherValues: Set<QualifiedValue<Q>> = vs2.values
        if (otherValues.isEmpty()) {
            if (vs1.values.isEmpty()) {
                return vs1
            }
            otherValues = UNDEFINED_AS_SET.values
        } else {
            if (vs1.values.isEmpty()) return vs2

            if (vs1._size != vs2._size) {
                // In case of merging of variables with different sizes (e.g. int and long)
                // both variables are not used after merge
                return EMPTY_VALUE_SET
            }
        }

        val mergedSet = UnifiedSetWithHashingStrategy<QualifiedValue<Q>>(TypedValueMergeHashingStrategy)

        mergedSet.addAll(vs1.values)
        mergedSet.addAll(otherValues)

        for (u in otherValues)
            for (v in vs1.values) {
                if (v identityEquals u)
                    continue

                val interestingInstances = u.base.interesting && v.base.interesting &&
                    u.base.parameterIndex == v.base.parameterIndex
                if (interestingInstances) {
                    mergedSet.remove(u)
                    if (v.qualifier != u.qualifier) {
                        mergedSet.remove(v)
                        mergedSet.add(v.copy(this.merge(v.qualifier, u.qualifier)))
                    }
                }
            }

        if (mergedSet == vs1.values) return vs1

        return QualifiedValueSet(vs1._size, mergedSet)
    }

    public override fun newValue(_type: Type?): QualifiedValueSet<Q> {
        if (_type == null || _type.sort == Type.VOID)
            return UNDEFINED_AS_SET

        val returnValueSlots = if (method.getReturnType() == VOID_TYPE) 0 else 1
        val thisSlots = if (method.isStatic()) 0 else 1

        val skip = thisSlots + returnValueSlots
        val interesting = valueSetsCreated in skip..method.getArgumentTypes().size() + skip - 1

        val parameterIndex = if (interesting) valueSetsCreated - returnValueSlots else null

        valueSetsCreated++

        val specialValue = specialValue(_type)
        return if (specialValue != null)
            specialValue
        else qualifiedValueSetOf(createValue(_type, parameterIndex, null))
    }

    public override fun newOperation(insn: AbstractInsnNode): QualifiedValueSet<Q> {
        return when (insn.opcode) {
            ACONST_NULL -> valueAtInstruction(NULL_TYPE, insn)
            ICONST_M1, ICONST_0, ICONST_1, ICONST_2, ICONST_3, ICONST_4, ICONST_5 -> valueAtInstruction(INT_TYPE, insn)
            LCONST_0, LCONST_1 -> valueAtInstruction(LONG_TYPE, insn)
            FCONST_0, FCONST_1, FCONST_2 -> valueAtInstruction(FLOAT_TYPE, insn)
            DCONST_0, DCONST_1 -> valueAtInstruction(DOUBLE_TYPE, insn)
            BIPUSH, SIPUSH -> valueAtInstruction(INT_TYPE, insn)
            LDC -> {
                val ldc = insn as LdcInsnNode
                val cst = ldc.cst
                fun illegalLdcConstant(): Throwable {
                    return IllegalArgumentException("Illegal LCD constant: $cst : ${cst!!.javaClass}")
                }
                when (cst) {
                    is Int -> valueAtInstruction(INT_TYPE, insn)
                    is Long -> valueAtInstruction(LONG_TYPE, insn)
                    is Float -> valueAtInstruction(FLOAT_TYPE, insn)
                    is Double -> valueAtInstruction(DOUBLE_TYPE, insn)
                    is String -> valueAtInstruction(Type.getObjectType("java/lang/String"), insn)
                    is Type -> {
                        when (cst.sort) {
                            OBJECT, ARRAY -> valueAtInstruction(Type.getObjectType("java/lang/Class"), insn)
                            METHOD -> valueAtInstruction(Type.getObjectType("java/lang/invoke/MethodType"), insn)
                            else -> throw illegalLdcConstant()
                        }
                    }
                    is Handle -> valueAtInstruction(Type.getObjectType("java/lang/invoke/MethodHandle"), insn)
                    else -> throw illegalLdcConstant()
                }

            }
            JSR -> RETURN_ADDRESS_VALUE_AS_SET
            GETSTATIC -> valueAtInstruction(Type.getType((insn as FieldInsnNode).desc), insn)
            NEW -> valueAtInstruction(Type.getObjectType((insn as TypeInsnNode).desc), insn)
            else -> throw unsupportedInstruction(insn)
        }
    }

    public override fun copyOperation(insn: AbstractInsnNode, value: QualifiedValueSet<Q>): QualifiedValueSet<Q> {
        return value
    }

    public override fun unaryOperation(insn: AbstractInsnNode, value: QualifiedValueSet<Q>): QualifiedValueSet<Q> {
        return when (insn.opcode) {
            INEG, IINC, L2I, F2I, D2I, I2B, I2C, I2S -> valueAtInstruction(INT_TYPE, insn)
            FNEG, I2F, L2F, D2F -> valueAtInstruction(FLOAT_TYPE, insn)
            LNEG, I2L, F2L, D2L -> valueAtInstruction(LONG_TYPE, insn)
            DNEG, I2D, L2D, F2D -> valueAtInstruction(DOUBLE_TYPE, insn)
            IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE, TABLESWITCH,
            LOOKUPSWITCH, IRETURN, LRETURN, FRETURN, DRETURN, ARETURN, PUTSTATIC -> UNDEFINED_AS_SET
            GETFIELD -> valueAtInstruction(Type.getType((insn as FieldInsnNode).desc), insn)
            NEWARRAY -> valueAtInstruction(primitiveArrayType((insn as IntInsnNode).operand), insn)
            ANEWARRAY -> valueAtInstruction(Type.getType("[" + Type.getObjectType((insn as TypeInsnNode).desc)), insn)
            ARRAYLENGTH -> valueAtInstruction(INT_TYPE, insn)
            ATHROW -> UNDEFINED_AS_SET
            CHECKCAST -> value
            INSTANCEOF -> valueAtInstruction(INT_TYPE, insn)
            MONITORENTER, MONITOREXIT, IFNULL, IFNONNULL -> UNDEFINED_AS_SET
            else -> throw unsupportedInstruction(insn)
        }
    }

    public override fun binaryOperation(
            insn: AbstractInsnNode, value1: QualifiedValueSet<Q>, value2: QualifiedValueSet<Q>
    ): QualifiedValueSet<Q> {
        return when (insn.opcode) {
            IALOAD, BALOAD, CALOAD, SALOAD, IADD, ISUB, IMUL,
            IDIV, IREM, ISHL, ISHR, IUSHR, IAND, IOR, IXOR -> valueAtInstruction(INT_TYPE, insn)
            FALOAD, FADD, FSUB, FMUL, FDIV, FREM -> valueAtInstruction(FLOAT_TYPE, insn)
            LALOAD, LADD, LSUB, LMUL, LDIV, LREM, LSHL, LSHR, LUSHR, LAND, LOR, LXOR -> valueAtInstruction(LONG_TYPE, insn)
            DALOAD, DADD, DSUB, DMUL, DDIV, DREM -> valueAtInstruction(DOUBLE_TYPE, insn)
            AALOAD -> valueAtInstruction(Type.getObjectType("java/lang/Object"), insn)
            LCMP, FCMPL, FCMPG, DCMPL, DCMPG -> valueAtInstruction(INT_TYPE, insn)
            IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE, IF_ACMPEQ, IF_ACMPNE, PUTFIELD -> UNDEFINED_AS_SET
            else -> throw unsupportedInstruction(insn)
        }
    }

    public override fun ternaryOperation(
            insn: AbstractInsnNode,
            value1: QualifiedValueSet<Q>, value2: QualifiedValueSet<Q>, value3: QualifiedValueSet<Q>
    ): QualifiedValueSet<Q>? {
        return UNDEFINED_AS_SET
    }

    public override fun naryOperation(insn: AbstractInsnNode, values: List<QualifiedValueSet<Q>>): QualifiedValueSet<Q>? {
        return when (insn) {
            is MultiANewArrayInsnNode -> valueAtInstruction(Type.getType(insn.desc), insn)
            is InvokeDynamicInsnNode -> valueAtInstruction(Type.getReturnType(insn.desc), insn)
            is MethodInsnNode -> valueAtInstruction(Type.getReturnType(insn.desc), insn)
            else -> throw unsupportedInstruction(insn)
        }
    }

    public override fun returnOperation(insn: AbstractInsnNode, value: QualifiedValueSet<Q>, expected: QualifiedValueSet<Q>) {}

    public override fun merge(v: QualifiedValueSet<Q>, w: QualifiedValueSet<Q>): QualifiedValueSet<Q> {
        return qualifierSet.merge(v, w)
    }
}

