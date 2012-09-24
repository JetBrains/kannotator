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
import org.jetbrains.kannotator.controlFlow.ControlFlowGraph
import edu.uci.ics.jung.graph.DirectedGraph
import edu.uci.ics.jung.algorithms.layout.SpringLayout
import edu.uci.ics.jung.algorithms.layout.SpringLayout2
import edu.uci.ics.jung.algorithms.layout.FRLayout
import edu.uci.ics.jung.algorithms.layout.KKLayout
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse
import edu.uci.ics.jung.visualization.VisualizationViewer
import java.awt.BorderLayout
import edu.uci.ics.jung.visualization.transform.shape.ViewLensSupport
import edu.uci.ics.jung.visualization.Layer
import edu.uci.ics.jung.visualization.transform.shape.MagnifyShapeTransformer
import edu.uci.ics.jung.visualization.control.ModalLensGraphMouse
import edu.uci.ics.jung.visualization.control.LensMagnificationGraphMousePlugin
import edu.uci.ics.jung.algorithms.shortestpath.MinimumSpanningForest2
import edu.uci.ics.jung.graph.DelegateForest
import edu.uci.ics.jung.graph.DelegateTree
import org.apache.commons.collections15.functors.ConstantTransformer
import edu.uci.ics.jung.algorithms.layout.TreeLayout
import edu.uci.ics.jung.algorithms.layout.StaticLayout
import java.awt.geom.Point2D


fun ControlFlowGraph.toJungGraph(): DirectedSparseMultigraph<Instruction, ControlFlowEdge> {
    val jungGraph = DirectedSparseMultigraph<Instruction, ControlFlowEdge>()
    for (i in this.instructions) {
        for (e in i.outgoingEdges) {
            jungGraph.addEdge(e, e.from, e.to, EdgeType.DIRECTED)
        }
    }
    return jungGraph
}

fun displayJungGraph(graph: DirectedGraph<Instruction, ControlFlowEdge>) {
    val layout = KKLayout(graph);
    layout.setSize(Dimension(800, 800)); // sets the initial size of the space
    // The BasicVisualizationServer<V,E> is parameterized by the edge types
    val prim = MinimumSpanningForestMaker.minimumSpanningForest(graph)
    val tree = prim.getForest();
    val treeLayout = TreeLayout(tree)
    val graphAsTree = StaticLayout(graph, treeLayout as Transformer<Instruction, Point2D?>)
    //    treeLayout.setSize(Dimension(800, 800))

    val vv = VisualizationViewer(graphAsTree);
    //    val vv = VisualizationViewer(layout);
    vv.setPreferredSize(Dimension(850, 850)); //Sets the viewing area size
    vv.getRenderContext()!!.setVertexLabelTransformer(object : Transformer<Instruction, String> {
        public override fun transform(instruction: Instruction): String = instruction.metadata.toString()
    })
    val gm = DefaultModalGraphMouse<Instruction, ControlFlowEdge>()
    vv.setGraphMouse(gm)

    //    MinimumSpanningForest2<Instruction, ControlFlowEdge>(
    //            graph,
    //            DelegateForest<Instruction, ControlFlowEdge?>(),
    //            DelegateTree.getFactory<Instruction, ControlFlowEdge?>(),
    //            ConstantTransformer<Double?>(1.0) as Transformer<ControlFlowEdge, Double>)


    //    val magnifyViewSupport =
    //        ViewLensSupport<Instruction, ControlFlowEdge>(vv, MagnifyShapeTransformer(vv,
    //        		vv.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.VIEW)),
    //                ModalLensGraphMouse(LensMagnificationGraphMousePlugin(1.toFloat(), 6.toFloat(), 0.2.toFloat())));
    //    magnifyViewSupport.activate(true)

    val frame = JFrame("Simple Graph View");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.getContentPane().add(vv, BorderLayout.CENTER);
    frame.getContentPane().add(gm.getModeComboBox(), BorderLayout.NORTH);

    frame.pack();
    frame.setVisible(true);
}
