package i2.act.grammargraph.properties;

import i2.act.grammargraph.*;
import i2.act.grammargraph.GrammarGraphNode.Choice;
import i2.act.grammargraph.GrammarGraphNode.Sequence;
import i2.act.util.Pair;

import java.util.HashMap;
import java.util.Map;

public final class ReachableChoicesComputation
    extends PropertyComputation<Map<Choice, Integer>> {

  private static final Map<Choice, Integer> UNKNOWN = null;

  public static final Map<GrammarGraphNode<?,?>, Map<Choice, Integer>> computeReachableChoices(
      final GrammarGraph grammarGraph) {
    final ReachableChoicesComputation computation = new ReachableChoicesComputation();
    return computation.compute(grammarGraph);
  }

  // -----------------------------------------------------------------------------------------------

  private ReachableChoicesComputation() {
    super(PropertyComputation.Direction.BACKWARDS);
  }

  private static final boolean requiresNode(final Choice choice) {
    return choice.hasGrammarSymbol() && choice.getGrammarSymbol().getProduction() != null;
  }

  @Override
  protected final Map<Choice, Integer> init(final Choice node,
      final GrammarGraph grammarGraph) {
    if (node.isLeaf()) {
      final Map<Choice, Integer> init = new HashMap<>();
      init.put(node, 0);
      return init;
    }

    return UNKNOWN;
  }

  @Override
  protected final Map<Choice, Integer> init(final Sequence node,
      final GrammarGraph grammarGraph) {
    return UNKNOWN;
  }

  @Override
  protected final Map<Choice, Integer> transfer(final Choice node,
      final Map<Choice, Integer> in) {
    if (in == UNKNOWN) {
      return in;
    }

    final boolean requiresNode = requiresNode(node);

    final Map<Choice, Integer> out = new HashMap<>();
    {
      for (final Map.Entry<Choice, Integer> entry : in.entrySet()) {
        final Choice reachableChoice = entry.getKey();
        final Integer distance = entry.getValue();

        final int newDistance = (requiresNode) ? (distance + 1) : (distance);

        out.put(reachableChoice, newDistance);
      }

      out.put(node, 0);
    }

    return out;
  }

  @Override
  protected final Map<Choice, Integer> transfer(final Sequence node,
      final Map<Choice, Integer> in) {
    return in;
  }

  @Override
  protected final Map<Choice, Integer> confluence(final Choice node,
      final Iterable<Pair<GrammarGraphEdge<?, ?>, Map<Choice, Integer>>> inSets) {
    return confluence(inSets);
  }

  @Override
  protected final Map<Choice, Integer> confluence(final Sequence node,
      final Iterable<Pair<GrammarGraphEdge<?, ?>, Map<Choice, Integer>>> inSets) {
    return confluence(inSets);
  }

  protected final Map<Choice, Integer> confluence(
      final Iterable<Pair<GrammarGraphEdge<?, ?>, Map<Choice, Integer>>> inSets) {
    final Map<Choice, Integer> out = new HashMap<>();

    for (final Pair<GrammarGraphEdge<?, ?>, Map<Choice, Integer>> inSet : inSets) {
      final Map<Choice, Integer> reachableChoices = inSet.getSecond();

      if (reachableChoices == UNKNOWN) {
        continue;
      }

      for (final Map.Entry<Choice, Integer> entry : reachableChoices.entrySet()) {
        final Choice reachableChoice = entry.getKey();
        final Integer distance = entry.getValue();

        if (!out.containsKey(reachableChoice) || distance < out.get(reachableChoice)) {
          out.put(reachableChoice, distance);
        }
      }
    }

    return out;
  }

}
