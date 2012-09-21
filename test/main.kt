import junit.framework.TestCase
import org.jetbrains.kannotator.controlFlow.ControlFlowGraphBuilder
import org.jetbrains.kannotator.controlFlow.InstructionMetadata
import edu.uci.ics.jung.graph.Graph
import edu.uci.ics.jung.graph.SparseMultigraph
import edu.uci.ics.jung.graph.util.EdgeType
import edu.uci.ics.jung.graph.DirectedOrderedSparseMultigraph
import org.jetbrains.kannotator.controlFlow.Instruction
import org.jetbrains.kannotator.controlFlow.ControlFlowEdge
import edu.uci.ics.jung.graph.DirectedSparseMultigraph
import edu.uci.ics.jung.algorithms.layout.CircleLayout
import java.awt.Dimension
import edu.uci.ics.jung.visualization.BasicVisualizationServer
import javax.swing.JFrame
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller
import org.apache.commons.collections15.Transformer


fun main(args: Array<String>) {
    val builder = ControlFlowGraphBuilder<String>()
    val a = builder.newInstruction(StringInstructionMetadata("a"))
    val b = builder.newInstruction(StringInstructionMetadata("b"))
    val c = builder.newInstruction(StringInstructionMetadata("c"))
    builder.setEntryPoint(a)
    builder.addEdge(a, b)
    builder.addEdge(b, c)
    builder.addEdge(c, a)
    val graph = builder.build()

    val jungGraph = DirectedSparseMultigraph<Instruction, ControlFlowEdge>()
    for (i in graph.instructions) {
        for (e in i.outgoingEdges) {
            jungGraph.addEdge(e, e.from, e.to, EdgeType.DIRECTED)
        }
    }

    val layout = CircleLayout(jungGraph);
    layout.setSize(Dimension(300, 300)); // sets the initial size of the space
    // The BasicVisualizationServer<V,E> is parameterized by the edge types
    val vv = BasicVisualizationServer(layout);
    vv.setPreferredSize(Dimension(350,350)); //Sets the viewing area size
    vv.getRenderContext()!!.setVertexLabelTransformer(object : Transformer<Instruction, String> {
        public override fun transform(instruction: Instruction): String = instruction.metadata.toString()
    })

    val frame = JFrame("Simple Graph View");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.getContentPane()?.add(vv);
    frame.pack();
    frame.setVisible(true);
}

private class StringInstructionMetadata(val data: String): InstructionMetadata {
    public fun toString(): String = data
}