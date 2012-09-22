import edu.uci.ics.jung.algorithms.shortestpath.MinimumSpanningForest2;
import edu.uci.ics.jung.graph.DelegateForest;
import edu.uci.ics.jung.graph.DelegateTree;
import edu.uci.ics.jung.graph.Graph;
import jet.runtime.typeinfo.KotlinSignature;
import org.apache.commons.collections15.functors.ConstantTransformer;

public class MinimumSpanningForestMaker {
    @KotlinSignature("fun minimumSpanningForest<V, E>(graph: Graph<V, E>): MinimumSpanningForest2<V, E>")
    public static <V, E> MinimumSpanningForest2<V, E> minimumSpanningForest(Graph<V, E> graph) {
        return new MinimumSpanningForest2<V, E>(
                graph,
                new DelegateForest<V, E>(),
                DelegateTree.<V, E>getFactory(),
                new ConstantTransformer(1.0)
        );
    }
}
