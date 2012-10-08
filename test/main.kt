import java.util.HashSet
import kotlinlib.buildString
import org.apache.commons.collections15.Transformer
import org.jetbrains.kannotator.controlFlow.ControlFlowEdge
import org.jetbrains.kannotator.controlFlow.Instruction
import org.jetbrains.kannotator.controlFlow.builder.STATE_BEFORE
import org.objectweb.asm.ClassReader
import util.controlFlow.buildControlFlowGraph

object InstructionTransformer : Transformer<Instruction, String> {
    public override fun transform(instruction: Instruction): String = instruction.metadata.toString()
}

object ControlFlowEdgeTransformer : Transformer<ControlFlowEdge, String> {
    public override fun transform(edge: ControlFlowEdge): String {
        val from = edge.from[STATE_BEFORE]
        val to = edge.to[STATE_BEFORE]
        if (to != null) {
            return buildString {
                sb ->
                // Difference between two states
                for (i in 0..to.localVariables.size - 1) {
                    val values = HashSet(to.localVariables[i])
                    if (from != null) {
                        values.removeAll(from.localVariables[i])
                    }
                    if (!values.isEmpty()) {
                        sb.append("l[$i] <= $values\n")
                    }
                }
            }
        }
        return ""
    }
}


fun main(args: Array<String>) {
    val classReader = ClassReader(javaClass<TestSubject>().getCanonicalName())
    val graph = buildControlFlowGraph(classReader, "foo", "(Ljava/lang/String;)V")

    displayJungGraph(graph.toJungGraph(), InstructionTransformer, ControlFlowEdgeTransformer)
}