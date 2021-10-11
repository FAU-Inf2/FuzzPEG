package i2.act.grammargraph.properties;

import i2.act.grammargraph.*;
import i2.act.grammargraph.GrammarGraphEdge.AlternativeEdge;
import i2.act.grammargraph.GrammarGraphEdge.SequenceEdge;
import i2.act.grammargraph.GrammarGraphEdge.SequenceEdge.Quantifier;
import i2.act.grammargraph.GrammarGraphNode.AlternativeNode;
import i2.act.grammargraph.GrammarGraphNode.SequenceNode;
import i2.act.util.Pair;

import java.util.Map;

public final class MinHeightComputation extends PropertyComputation<Integer> {

  private static final Integer UNKNOWN = Integer.MAX_VALUE;

  public static final Map<GrammarGraphNode<?,?>, Integer> computeMinHeights(
      final GrammarGraph grammarGraph) {
    final MinHeightComputation computation = new MinHeightComputation();
    return computation.compute(grammarGraph);
  }

  // -----------------------------------------------------------------------------------------------

  private MinHeightComputation() {
    super(PropertyComputation.Direction.BACKWARDS);
  }

  @Override
  protected final Integer init(final AlternativeNode node, final GrammarGraph grammarGraph) {
    if (node.isLeaf()) {
      return 1;
    } else {
      return UNKNOWN;
    }
  }

  @Override
  protected final Integer init(final SequenceNode node, final GrammarGraph grammarGraph) {
    return UNKNOWN;
  }

  @Override
  protected final Integer transfer(final AlternativeNode node, final Integer in) {
    if (in == UNKNOWN) {
      return UNKNOWN;
    }

    if (!node.hasGrammarSymbol() || node.getGrammarSymbol().getProduction() == null) {
      // helper node
      return in;
    } else {
      return in + 1;
    }
  }

  @Override
  protected final Integer transfer(final SequenceNode node, final Integer in) {
    return in;
  }

  @Override
  protected final Integer confluence(final AlternativeNode node,
      final Iterable<Pair<GrammarGraphEdge<?, ?>, Integer>> inSets) {
    Integer minMinHeight = UNKNOWN;

    for (final Pair<GrammarGraphEdge<?, ?>, Integer> inSet : inSets) {
      assert (inSet.getFirst() instanceof AlternativeEdge);
      final AlternativeEdge edge = (AlternativeEdge) inSet.getFirst();
      final Integer minHeight = inSet.getSecond();

      if (minHeight < minMinHeight) {
        minMinHeight = minHeight;
      }
    }

    return minMinHeight;
  }

  @Override
  protected final Integer confluence(final SequenceNode node,
      final Iterable<Pair<GrammarGraphEdge<?, ?>, Integer>> inSets) {
    int maxMinHeight = 0;

    for (final Pair<GrammarGraphEdge<?, ?>, Integer> inSet : inSets) {
      assert (inSet.getFirst() instanceof SequenceEdge);
      final SequenceEdge edge = (SequenceEdge) inSet.getFirst();
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
