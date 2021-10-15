package i2.act.grammargraph.properties;

import i2.act.grammargraph.*;
import i2.act.grammargraph.GrammarGraphNode.Choice;
import i2.act.grammargraph.GrammarGraphNode.Sequence;
import i2.act.peg.symbols.LexerSymbol;
import i2.act.util.Pair;

import java.util.Iterator;
import java.util.Map;

public final class ReachableComputation extends PropertyComputation<Boolean> {

  public static final Map<GrammarGraphNode<?,?>, Boolean> computeReachable(
      final GrammarGraph grammarGraph, final boolean considerSkippedTokensReachable) {
    final ReachableComputation computation =
        new ReachableComputation(considerSkippedTokensReachable);
    return computation.compute(grammarGraph);
  }

  // -----------------------------------------------------------------------------------------------

  private final boolean considerSkippedTokensReachable;

  private ReachableComputation(final boolean considerSkippedTokensReachable) {
    super(PropertyComputation.Direction.FORWARDS);
    this.considerSkippedTokensReachable = considerSkippedTokensReachable;
  }

  @Override
  protected final Boolean init(final Choice node, final GrammarGraph grammarGraph) {
    if (node == grammarGraph.getRootNode()) {
      return true;
    }

    if (node.hasGrammarSymbol() && (node.getGrammarSymbol() instanceof LexerSymbol)) {
      final LexerSymbol lexerSymbol = (LexerSymbol) node.getGrammarSymbol();

      if ((lexerSymbol == LexerSymbol.EOF)
          || (this.considerSkippedTokensReachable && lexerSymbol.isSkippedToken())) {
        return true;
      }
    }

    return false;
  }

  @Override
  protected final Boolean init(final Sequence node, final GrammarGraph grammarGraph) {
    return false;
  }

  @Override
  protected final Boolean transfer(final Choice node, final Boolean in) {
    return in;
  }

  @Override
  protected final Boolean transfer(final Sequence node, final Boolean in) {
    return in;
  }

  @Override
  protected final Boolean confluence(final Choice node,
      final Iterable<Pair<GrammarGraphEdge<?, ?>, Boolean>> inSets) {
    for (final Pair<GrammarGraphEdge<?, ?>, Boolean> inSet : inSets) {
      final Boolean successorReachable = inSet.getSecond();

      if (successorReachable) {
        return true;
      }
    }

    return false;
  }

  @Override
  protected final Boolean confluence(final Sequence node,
      final Iterable<Pair<GrammarGraphEdge<?, ?>, Boolean>> inSets) {
    final Iterator<Pair<GrammarGraphEdge<?, ?>, Boolean>> inSetsIterator = inSets.iterator();

    // there must be exactly one incoming edge
    assert (inSetsIterator.hasNext());

    final Boolean inValue = inSetsIterator.next().getSecond();

    // there must be exactly one incoming edge
    assert (!inSetsIterator.hasNext());

    return inValue;
  }

}
