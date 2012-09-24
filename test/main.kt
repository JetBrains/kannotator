import org.jetbrains.kannotator.controlFlowBuilder.buildControlFlowGraph
import org.objectweb.asm.ClassReader

fun main(args: Array<String>) {
    val classReader = ClassReader(javaClass<TestSubject>().getCanonicalName())
    val graph = buildControlFlowGraph(classReader, "foo", "(Ljava/lang/String;)V")

    displayJungGraph(graph.toJungGraph())
}