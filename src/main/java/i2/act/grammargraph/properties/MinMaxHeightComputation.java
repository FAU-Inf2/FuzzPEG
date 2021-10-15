package i2.act.grammargraph.properties;

import i2.act.grammargraph.GrammarGraph;
import i2.act.grammargraph.GrammarGraphNode;
import i2.act.grammargraph.GrammarGraphNode.Choice;

import java.util.Map;

public final class MinMaxHeightComputation {

  public static final int computeMinMaxHeight(final GrammarGraph grammarGraph) {
    final Map<GrammarGraphNode<?, ?>, Integer> minHeights =
        MinHeightComputation.computeMinHeights(grammarGraph);

    final Map<GrammarGraphNode<?, ?>, Integer> minDepths =
        MinDepthComputation.computeMinDepths(grammarGraph);

    final Map<GrammarGraphNode<?, ?>, Boolean> reachable =
        ReachableComputation.computeReachable(grammarGraph, false);

    int minMaxHeight = 0;

    for (final Map.Entry<GrammarGraphNode<?,?>, Boolean> entry : reachable.entrySet()) {
      final GrammarGraphNode<?,?> node = entry.getKey();
      final Boolean isReachable = entry.getValue();

      if ((!isReachable) || !(node instanceof Choice)) {
        continue;
      }

      assert (minHeights.containsKey(node));
      assert (minDepths.containsKey(node));

      final int minMaxHeightNode = minHeights.get(node) + minDepths.get(node);

      if (minMaxHeightNode > minMaxHeight) {
        minMaxHeight = minMaxHeightNode;
      }
    }

    return minMaxHeight;
  }

}
