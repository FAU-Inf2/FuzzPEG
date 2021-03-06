package i2.act.grammargraph.properties;

import i2.act.grammargraph.*;
import i2.act.grammargraph.GrammarGraphEdge.Alternative;
import i2.act.grammargraph.GrammarGraphEdge.Element;
import i2.act.grammargraph.GrammarGraphEdge.Element.Quantifier;
import i2.act.grammargraph.GrammarGraphNode.Choice;
import i2.act.grammargraph.GrammarGraphNode.Sequence;
import i2.act.util.Pair;

import java.util.Map;

public final class MinSizeComputation extends PropertyComputation<Integer> {

  private static final Integer UNKNOWN = Integer.MAX_VALUE;

  public static final Map<GrammarGraphNode<?,?>, Integer> computeMinSizes(
      final GrammarGraph grammarGraph) {
    final MinSizeComputation computation = new MinSizeComputation();
    return computation.compute(grammarGraph);
  }

  // -----------------------------------------------------------------------------------------------

  private MinSizeComputation() {
    super(PropertyComputation.Direction.BACKWARDS);
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

    if (!node.hasGrammarSymbol() || node.getGrammarSymbol().getProduction() == null) {
      // helper node
      return in;
    } else {
      return in + 1;
    }
  }

  @Override
  protected final Integer transfer(final Sequence node, final Integer in) {
    return in;
  }

  @Override
  protected final Integer confluence(final Choice node,
      final Iterable<Pair<GrammarGraphEdge<?, ?>, Integer>> inSets) {
    Integer minMinSize = UNKNOWN;

    for (final Pair<GrammarGraphEdge<?, ?>, Integer> inSet : inSets) {
      assert (inSet.getFirst() instanceof Alternative);
      final Alternative edge = (Alternative) inSet.getFirst();
      final Integer minSize = inSet.getSecond();

      if (minSize < minMinSize) {
        minMinSize = minSize;
      }
    }

    return minMinSize;
  }

  @Override
  protected final Integer confluence(final Sequence node,
      final Iterable<Pair<GrammarGraphEdge<?, ?>, Integer>> inSets) {
    int minSizeSequence = 0;

    for (final Pair<GrammarGraphEdge<?, ?>, Integer> inSet : inSets) {
      assert (inSet.getFirst() instanceof Element);
      final Element edge = (Element) inSet.getFirst();
      final Integer minSizeElement = inSet.getSecond();

      final Quantifier quantifier = edge.getQuantifier();
      if (quantifier == Quantifier.QUANT_OPTIONAL || quantifier == Quantifier.QUANT_STAR) {
        continue;
      }

      assert (quantifier == Quantifier.QUANT_NONE || quantifier == Quantifier.QUANT_PLUS);

      if (minSizeElement == UNKNOWN) {
        return UNKNOWN;
      }

      minSizeSequence += minSizeElement;
    }

    return minSizeSequence;
  }

}
