package i2.act.grammargraph.properties;

import i2.act.grammargraph.GrammarGraph;
import i2.act.grammargraph.GrammarGraphNode;

import java.util.Map;

public final class MinMaxHeightComputation {

  public static final int computeMinMaxHeight(final GrammarGraph grammarGraph) {
    final Map<GrammarGraphNode<?,?>, Map<GrammarGraphNode<?,?>, Integer>> reachableNodes =
        ReachableNodesComputation.computeReachableNodes(grammarGraph);

    final GrammarGraphNode<?,?> rootNode = grammarGraph.getRootNode();
    assert (reachableNodes.containsKey(rootNode));

    final Map<GrammarGraphNode<?,?>, Integer> reachableFromRootNode = reachableNodes.get(rootNode);

    int minMaxHeight = 0;

    for (final Integer requiredHeight : reachableFromRootNode.values()) {
      if (requiredHeight > minMaxHeight) {
        minMaxHeight = requiredHeight;
      }
    }

    return minMaxHeight;
  }

}
