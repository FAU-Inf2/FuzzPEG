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
    final ReachableComputation computation = new ReachableComputation(
        grammarGraph.getRootNode(), considerSkippedTokensReachable);
    return computation.compute(grammarGraph);
  }

  // -----------------------------------------------------------------------------------------------

  private final Choice rootNode;
  private final boolean considerSkippedTokensReachable;

  private ReachableComputation(final Choice rootNode,
      final boolean considerSkippedTokensReachable) {
    super(PropertyComputation.Direction.FORWARDS);
    this.rootNode = rootNode;
    this.considerSkippedTokensReachable = considerSkippedTokensReachable;
  }

  @Override
  protected final Boolean init(final Choice node, final GrammarGraph grammarGraph) {
    if (node == this.rootNode) {
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
    // root node might have incoming edges
    return in || (node == this.rootNode);
  }

  @Override
  protected final Boolean transfer(final Sequence node, final Boolean in) {
    return in;
  }

  @Override
  protected final Boolean confluence(final Choice node,
      final Iterable<Pair<GrammarGraphEdge<?, ?>, Boolean>> inSets) {
    for (final Pair<GrammarGraphEdge<?, ?>, Boolean> inSet : inSets) {
      final Boolean predecessorReachable = inSet.getSecond();

      if (predecessorReachable) {
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
