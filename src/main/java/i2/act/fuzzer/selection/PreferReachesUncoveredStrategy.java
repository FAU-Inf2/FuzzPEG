package i2.act.fuzzer.selection;

import i2.act.coverage.AlternativeCoverage;
import i2.act.grammargraph.GrammarGraph;
import i2.act.grammargraph.GrammarGraphEdge.Alternative;
import i2.act.grammargraph.GrammarGraphEdge.Element;
import i2.act.grammargraph.GrammarGraphNode;
import i2.act.grammargraph.GrammarGraphNode.Choice;
import i2.act.grammargraph.GrammarGraphNode.Sequence;
import i2.act.grammargraph.properties.MinHeightComputation;
import i2.act.grammargraph.properties.ReachableNodesComputation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class PreferReachesUncoveredStrategy implements SelectionStrategy {

  private final Map<GrammarGraphNode<?,?>, Map<GrammarGraphNode<?,?>, Integer>> reachableNodes;
  private final Map<GrammarGraphNode<?,?>, Integer> minHeights;

  private final AlternativeCoverage coverage;
  private final SelectionStrategy strategyUncovered;
  private final SelectionStrategy strategyCovered;

  public PreferReachesUncoveredStrategy(final GrammarGraph grammarGraph,
      final AlternativeCoverage coverage,
      final SelectionStrategy strategyUncovered, final SelectionStrategy strategyCovered) {
    this.reachableNodes = ReachableNodesComputation.computeReachableNodes(grammarGraph);
    this.minHeights = MinHeightComputation.computeMinHeights(grammarGraph);
    this.coverage = coverage;
    this.strategyUncovered = strategyUncovered;
    this.strategyCovered = strategyCovered;
  }

  private static final boolean requiresNode(final Choice choice) {
    return choice.hasGrammarSymbol() && choice.getGrammarSymbol().getProduction() != null;
  }

  private final boolean reachesUncoveredAlternative(final Choice choice, final int maxHeight) {
    final int childHeight = (requiresNode(choice)) ? (maxHeight - 1) : (maxHeight);

    for (final Alternative alternative : choice.getSuccessorEdges()) {
      if (reachesUncoveredAlternative(alternative, childHeight)) {
        return true;
      }
    }

    return false;
  }

  private final boolean reachesUncoveredAlternative(final Alternative alternative,
      final int maxHeight) {
    final Sequence sequence = alternative.getTarget();

    assert (this.reachableNodes.containsKey(sequence));
    assert (this.minHeights.containsKey(sequence));

    if (!this.coverage.isCovered(alternative)) {
      final int minHeightSequence = this.minHeights.get(sequence);

      if (minHeightSequence <= maxHeight) {
        // TODO remove
        //System.out.println("(1) reaches uncovered alternative of "
        //    + alternative.getSource().getGrammarSymbol());

        return true;
      }
    }

    // alternative has already been covered (or height limit does not suffice to cover alternative),
    // but it may lead to other, uncovered alternatives

    final Map<GrammarGraphNode<?,?>, Integer> reachableNodesAlternative =
        this.reachableNodes.get(sequence);

    for (final Map.Entry<GrammarGraphNode<?,?>, Integer> entry :
        reachableNodesAlternative.entrySet()) {
      final GrammarGraphNode<?,?> reachableNode = entry.getKey();

      if (!(reachableNode instanceof Sequence)) {
        continue;
      }

      final Sequence reachableSequence = (Sequence) reachableNode;
      final Integer minHeightReachableSequence = entry.getValue();

      assert (reachableNode.numberOfPredecessors() == 1);
      final Alternative alternativeToReachableSequence =
          reachableSequence.getPredecessorEdges().get(0);

      if (!this.coverage.isCovered(alternativeToReachableSequence)
          && minHeightReachableSequence <= maxHeight) {
        // TODO remove
        //System.out.println("(2) reaches uncovered alternative of "
        //    + alternativeToReachableSequence.getSource().getGrammarSymbol());

        return true;
      }
    }

    return false;
  }

  @Override
  public final Alternative chooseAlternative(final List<Alternative> alternatives,
      final int maxHeight) {
    final List<Alternative> preferedAlternatives = new ArrayList<>();

    for (final Alternative alternative : alternatives) {
      if (reachesUncoveredAlternative(alternative, maxHeight)) {
        preferedAlternatives.add(alternative);
      }
    }

    if (preferedAlternatives.isEmpty()) {
      return this.strategyCovered.chooseAlternative(alternatives, maxHeight);
    } else {
      return this.strategyUncovered.chooseAlternative(preferedAlternatives, maxHeight);
    }
  }

  @Override
  public final boolean generateMoreElements(final Element element, final int count,
      final int maxHeight) {
    // TODO remove
    if (false) {
      final String parentChoiceName =
          element.getSource().getPredecessorEdges().get(0).getSource().getGrammarSymbol().getName();

      for (int c = 0; c < (40 - maxHeight); ++c) {
        System.out.print(".");
      }

      System.out.format("%s (%d, %d): %d\n",
          parentChoiceName, count, maxHeight, this.coverage.missingCount());

      reachesUncoveredAlternative(element.getTarget(), maxHeight);
    }

    if (reachesUncoveredAlternative(element.getTarget(), maxHeight)) {
      return this.strategyUncovered.generateMoreElements(element, count, maxHeight);
    } else {
      return this.strategyCovered.generateMoreElements(element, count, maxHeight);
    }
  }

}
