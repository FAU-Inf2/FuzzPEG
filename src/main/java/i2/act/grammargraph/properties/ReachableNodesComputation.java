package i2.act.grammargraph.properties;

import i2.act.grammargraph.*;
import i2.act.grammargraph.GrammarGraphEdge.Element;
import i2.act.grammargraph.GrammarGraphNode.Choice;
import i2.act.grammargraph.GrammarGraphNode.Sequence;
import i2.act.util.Pair;

import java.util.HashMap;
import java.util.Map;

public final class ReachableNodesComputation
    extends PropertyComputation<Map<GrammarGraphNode<?,?>, Integer>> {

  private static final Map<GrammarGraphNode<?,?>, Integer> UNKNOWN = null;

  public static final Map<GrammarGraphNode<?,?>, Map<GrammarGraphNode<?,?>, Integer>>
      computeReachableNodes(final GrammarGraph grammarGraph) {
    final ReachableNodesComputation computation = new ReachableNodesComputation(grammarGraph);
    return computation.compute(grammarGraph);
  }

  // -----------------------------------------------------------------------------------------------

  private final Map<GrammarGraphNode<?, ?>, Integer> minHeights;

  private ReachableNodesComputation(final GrammarGraph grammarGraph) {
    super(PropertyComputation.Direction.BACKWARDS);
    this.minHeights = MinHeightComputation.computeMinHeights(grammarGraph);
  }

  private static final boolean requiresNode(final Choice choice) {
    return choice.hasGrammarSymbol() && choice.getGrammarSymbol().getProduction() != null;
  }

  @Override
  protected final Map<GrammarGraphNode<?,?>, Integer> init(final Choice node,
      final GrammarGraph grammarGraph) {
    if (node.isLeaf()) {
      final Map<GrammarGraphNode<?,?>, Integer> init = new HashMap<>();
      init.put(node, 1);
      return init;
    }

    return UNKNOWN;
  }

  @Override
  protected final Map<GrammarGraphNode<?,?>, Integer> init(final Sequence node,
      final GrammarGraph grammarGraph) {
    return UNKNOWN;
  }

  @Override
  protected final Map<GrammarGraphNode<?,?>, Integer> transfer(final Choice node,
      final Map<GrammarGraphNode<?,?>, Integer> in) {
    if (in == UNKNOWN) {
      return in;
    }

    final boolean requiresNode = requiresNode(node);

    final Map<GrammarGraphNode<?,?>, Integer> out = new HashMap<>();
    {
      for (final Map.Entry<GrammarGraphNode<?,?>, Integer> entry : in.entrySet()) {
        final GrammarGraphNode<?,?> reachableNode = entry.getKey();
        final Integer minHeightNode = entry.getValue();

        final int newMinHeightNode = (requiresNode) ? (minHeightNode + 1) : (minHeightNode);

        out.put(reachableNode, newMinHeightNode);
      }

      assert (this.minHeights.containsKey(node));
      out.put(node, this.minHeights.get(node));
    }

    return out;
  }

  @Override
  protected final Map<GrammarGraphNode<?,?>, Integer> transfer(final Sequence node,
      final Map<GrammarGraphNode<?,?>, Integer> in) {
    final Map<GrammarGraphNode<?,?>, Integer> out;
    {
      if (in == null) {
        out = new HashMap<>();
      } else {
        out = new HashMap<>(in);
      }
    }

    assert (this.minHeights.containsKey(node));
    final int minHeightSequence = this.minHeights.get(node);

    out.put(node, minHeightSequence);

    return out;
  }

  @Override
  protected final Map<GrammarGraphNode<?,?>, Integer> confluence(final Choice node,
      final Iterable<Pair<GrammarGraphEdge<?, ?>, Map<GrammarGraphNode<?,?>, Integer>>> inSets) {
    final Map<GrammarGraphNode<?,?>, Integer> in = new HashMap<>();

    for (final Pair<GrammarGraphEdge<?, ?>, Map<GrammarGraphNode<?,?>, Integer>> inSet : inSets) {
      final Map<GrammarGraphNode<?,?>, Integer> reachableNodes = inSet.getSecond();

      if (reachableNodes == UNKNOWN) {
        continue;
      }

      for (final Map.Entry<GrammarGraphNode<?,?>, Integer> entry : reachableNodes.entrySet()) {
        final GrammarGraphNode<?,?> reachableNode = entry.getKey();
        final Integer minHeightNode = entry.getValue();

        if (!in.containsKey(reachableNode) || minHeightNode < in.get(reachableNode)) {
          in.put(reachableNode, minHeightNode);
        }
      }
    }

    return in;
  }

  @Override
  protected final Map<GrammarGraphNode<?,?>, Integer> confluence(final Sequence node,
      final Iterable<Pair<GrammarGraphEdge<?, ?>, Map<GrammarGraphNode<?,?>, Integer>>> inSets) {
    assert (this.minHeights.containsKey(node));
    final int minHeightSequence = this.minHeights.get(node);

    final Map<GrammarGraphNode<?,?>, Integer> in = new HashMap<>();

    for (final Pair<GrammarGraphEdge<?, ?>, Map<GrammarGraphNode<?,?>, Integer>> inSet : inSets) {
      assert (inSet.getFirst() instanceof Element);
      final Element element = (Element) inSet.getFirst();

      final Map<GrammarGraphNode<?,?>, Integer> reachableNodes = inSet.getSecond();

      if (reachableNodes == UNKNOWN) {
        continue;
      }

      assert (this.minHeights.containsKey(element.getTarget()));
      final int minHeightElement = this.minHeights.get(element.getTarget());

      for (final Map.Entry<GrammarGraphNode<?,?>, Integer> entry : reachableNodes.entrySet()) {
        final GrammarGraphNode<?,?> reachableNode = entry.getKey();
        final Integer minHeightNode = entry.getValue();

        final int newMinHeightNode =
            Math.max(Math.max(minHeightSequence, minHeightElement), minHeightNode);

        if (!in.containsKey(reachableNode) || newMinHeightNode < in.get(reachableNode)) {
          in.put(reachableNode, newMinHeightNode);
        }
      }
    }

    return in;
  }

}
