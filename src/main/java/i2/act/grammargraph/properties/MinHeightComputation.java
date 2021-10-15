package i2.act.grammargraph.properties;

import i2.act.grammargraph.*;
import i2.act.grammargraph.GrammarGraphEdge.Alternative;
import i2.act.grammargraph.GrammarGraphEdge.Element;
import i2.act.grammargraph.GrammarGraphEdge.Element.Quantifier;
import i2.act.grammargraph.GrammarGraphNode.Choice;
import i2.act.grammargraph.GrammarGraphNode.Sequence;
import i2.act.util.Pair;

import java.util.Map;

public final class MinHeightComputation extends PropertyComputation<Integer> {

  public static enum Mode {
    SINGLE_ALTERNATIVE,
    ALL_ALTERNATIVES;
  }

  private static final Integer UNKNOWN = Integer.MAX_VALUE;

  public static final Map<GrammarGraphNode<?,?>, Integer> computeMinHeights(
      final GrammarGraph grammarGraph) {
    return computeMinHeights(grammarGraph, Mode.SINGLE_ALTERNATIVE);
  }

  public static final Map<GrammarGraphNode<?,?>, Integer> computeMinHeights(
      final GrammarGraph grammarGraph, final Mode mode) {
    final MinHeightComputation computation = new MinHeightComputation();
    final Map<GrammarGraphNode<?,?>, Integer> minHeights = computation.compute(grammarGraph);

    if (mode == Mode.ALL_ALTERNATIVES) {
      for (final GrammarGraphNode<?,?> node : minHeights.keySet()) {
        if (!(node instanceof Choice)) {
          continue;
        }

        final Choice choice = (Choice) node;

        int maxMinHeight = 0;
        for (final Alternative alternative : choice.getSuccessorEdges()) {
          assert (minHeights.containsKey(alternative.getTarget()));
          final int minHeightAlternative = minHeights.get(alternative.getTarget());

          if (minHeightAlternative > maxMinHeight) {
            maxMinHeight = minHeightAlternative;
          }
        }

        if (requiresNode(choice)) {
          maxMinHeight += 1;
        }

        minHeights.put(choice, maxMinHeight);
      }
    }

    return minHeights;
  }

  // -----------------------------------------------------------------------------------------------

  private MinHeightComputation() {
    super(PropertyComputation.Direction.BACKWARDS);
  }

  private static final boolean requiresNode(final Choice choice) {
    return choice.hasGrammarSymbol() && choice.getGrammarSymbol().getProduction() != null;
  }

  @Override
  protected final Integer init(final Choice node, final GrammarGraph grammarGraph) {
    if (node.isLeaf()) {
      return 1;
    } else {
      return UNKNOWN;
    }
  }

  @Override
  protected final Integer init(final Sequence node, final GrammarGraph grammarGraph) {
    return UNKNOWN;
  }

  @Override
  protected final Integer transfer(final Choice node, final Integer in) {
    if (in == UNKNOWN) {
      return UNKNOWN;
    }

    if (requiresNode(node)) {
      return in + 1;
    } else {
      // helper node
      return in;
    }
  }

  @Override
  protected final Integer transfer(final Sequence node, final Integer in) {
    return in;
  }

  @Override
  protected final Integer confluence(final Choice node,
      final Iterable<Pair<GrammarGraphEdge<?, ?>, Integer>> inSets) {
    Integer minMinHeight = UNKNOWN;

    for (final Pair<GrammarGraphEdge<?, ?>, Integer> inSet : inSets) {
      assert (inSet.getFirst() instanceof Alternative);
      final Alternative edge = (Alternative) inSet.getFirst();
      final Integer minHeight = inSet.getSecond();

      if (minHeight < minMinHeight) {
        minMinHeight = minHeight;
      }
    }

    return minMinHeight;
  }

  @Override
  protected final Integer confluence(final Sequence node,
      final Iterable<Pair<GrammarGraphEdge<?, ?>, Integer>> inSets) {
    int maxMinHeight = 0;

    for (final Pair<GrammarGraphEdge<?, ?>, Integer> inSet : inSets) {
      assert (inSet.getFirst() instanceof Element);
      final Element edge = (Element) inSet.getFirst();
      final Integer minHeight = inSet.getSecond();

      final Quantifier quantifier = edge.getQuantifier();
      if (quantifier == Quantifier.QUANT_OPTIONAL || quantifier == Quantifier.QUANT_STAR) {
        continue;
      }

      assert (quantifier == Quantifier.QUANT_NONE || quantifier == Quantifier.QUANT_PLUS);

      if (minHeight == UNKNOWN) {
        return UNKNOWN;
      }

      if (minHeight > maxMinHeight) {
        maxMinHeight = minHeight;
      }
    }

    return maxMinHeight;
  }

}
