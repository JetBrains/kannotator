package tests.funDependency

import edu.uci.ics.jung.algorithms.layout.KKLayout
import edu.uci.ics.jung.algorithms.layout.StaticLayout
import edu.uci.ics.jung.algorithms.layout.TreeLayout
import edu.uci.ics.jung.graph.DirectedGraph
import edu.uci.ics.jung.graph.DirectedSparseMultigraph
import edu.uci.ics.jung.graph.util.EdgeType
import edu.uci.ics.jung.visualization.VisualizationViewer
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.geom.Point2D
import javax.swing.JFrame
import org.apache.commons.collections15.Transformer
import org.jetbrains.kannotator.funDependecy.FunDependencyEdge
import org.jetbrains.kannotator.funDependecy.FunDependencyGraph
import org.jetbrains.kannotator.funDependecy.FunctionNode
import org.jetbrains.kannotator.funDependecy.FunctionNodeImpl
import org.jetbrains.kannotator.funDependecy.addEdge
import org.objectweb.asm.ClassReader

fun buildFunctionDependencyGraph(classReader: ClassReader) : FunDependencyGraph {
    val node1 = FunctionNodeImpl("First")
    val node2 = FunctionNodeImpl("Second")
    addEdge(node1, node2)

    return object : FunDependencyGraph {
        override val functions: Collection<FunctionNode> = arrayList(node1, node2)
    }
}

fun FunDependencyGraph.toJungGraph(): DirectedSparseMultigraph<FunctionNode, FunDependencyEdge> {
    val jungGraph = DirectedSparseMultigraph<FunctionNode, FunDependencyEdge>()
    for (i in this.functions) {
        for (e in i.outgoingEdges) {
            jungGraph.addEdge(e, e.from, e.to, EdgeType.DIRECTED)
        }
    }
    return jungGraph
}

fun displayJungGraph(graph: DirectedGraph<FunctionNode, FunDependencyEdge>) {
    val layout = KKLayout(graph);
    layout.setSize(Dimension(800, 800));
    // The BasicVisualizationServer<V,E> is parameterized by the edge types
    val prim = MinimumSpanningForestMaker.minimumSpanningForest(graph)
    val tree = prim.getForest();
    val treeLayout = TreeLayout(tree)
    val graphAsTree = StaticLayout(graph, treeLayout as Transformer<FunctionNode, Point2D?>)

    val vv = VisualizationViewer(graphAsTree);

    vv.setPreferredSize(Dimension(850, 850)); //Sets the viewing area size
    vv.getRenderContext()!!.setVertexLabelTransformer(object : Transformer<FunctionNode, String> {
        public override fun transform(functionNode: FunctionNode): String = functionNode.name
    })

    val gm = DefaultModalGraphMouse<FunctionNode, FunDependencyEdge>()
    vv.setGraphMouse(gm)

    val frame = JFrame("Simple Graph View");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.getContentPane()!!.add(vv, BorderLayout.CENTER);
    frame.getContentPane()!!.add(gm.getModeComboBox(), BorderLayout.NORTH);

    frame.pack();
    frame.setVisible(true);
}

fun main(args: Array<String>) {
    val graph = buildFunctionDependencyGraph(ClassReader(javaClass<TestSubject>().getCanonicalName()))
    displayJungGraph(graph.toJungGraph())
}