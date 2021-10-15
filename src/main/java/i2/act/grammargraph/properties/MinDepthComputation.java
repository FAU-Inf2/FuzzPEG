package i2.act.grammargraph.properties;

import i2.act.grammargraph.*;
import i2.act.grammargraph.GrammarGraphNode.Choice;
import i2.act.grammargraph.GrammarGraphNode.Sequence;
import i2.act.util.Pair;

import java.util.Iterator;
import java.util.Map;

public final class MinDepthComputation extends PropertyComputation<Integer> {

  private static final Integer UNKNOWN = Integer.MAX_VALUE;

  public static final Map<GrammarGraphNode<?,?>, Integer> computeMinDepths(
      final GrammarGraph grammarGraph) {
    final MinDepthComputation computation = new MinDepthComputation(grammarGraph);
    return computation.compute(grammarGraph);
  }

  // -----------------------------------------------------------------------------------------------

  private final GrammarGraph grammarGraph;

  private MinDepthComputation(final GrammarGraph grammarGraph) {
    super(PropertyComputation.Direction.FORWARDS);
    this.grammarGraph = grammarGraph;
  }

  @Override
  protected final Integer init(final Choice node, final GrammarGraph grammarGraph) {
    if (node == grammarGraph.getRootNode() || node.isRoot()) {
      return 0;
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

    if (node == this.grammarGraph.getRootNode()) {
      // root note might have incoming edges
      return 0;
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
    Integer minMinDepth = UNKNOWN;

    for (final Pair<GrammarGraphEdge<?, ?>, Integer> inSet : inSets) {
      final Integer minDepth = inSet.getSecond();

      if (minDepth < minMinDepth) {
        minMinDepth = minDepth;
      }
    }

    return minMinDepth;
  }

  @Override
  protected final Integer confluence(final Sequence node,
      final Iterable<Pair<GrammarGraphEdge<?, ?>, Integer>> inSets) {
    final Iterator<Pair<GrammarGraphEdge<?, ?>, Integer>> inSetsIterator = inSets.iterator();

    // there must be exactly one incoming edge
    assert (inSetsIterator.hasNext());

    final Integer inValue = inSetsIterator.next().getSecond();

    // there must be exactly one incoming edge
    assert (!inSetsIterator.hasNext());

    return inValue;
  }

}
