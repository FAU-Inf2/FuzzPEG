package i2.act.fuzzer.selection;

import i2.act.coverage.AlternativeCoverage;
import i2.act.grammargraph.GrammarGraph;
import i2.act.grammargraph.GrammarGraphEdge.Alternative;
import i2.act.grammargraph.GrammarGraphEdge.Element;
import i2.act.grammargraph.GrammarGraphNode;
import i2.act.grammargraph.GrammarGraphNode.Choice;
import i2.act.grammargraph.GrammarGraphNode.Sequence;
import i2.act.grammargraph.properties.MinHeightComputation;
import i2.act.grammargraph.properties.ReachableChoicesComputation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class PreferReachesUncoveredStrategy implements SelectionStrategy {

  private final Map<GrammarGraphNode<?,?>, Map<Choice, Integer>> reachableChoices;
  private final Map<GrammarGraphNode<?,?>, Integer> minHeights;

  private final AlternativeCoverage coverage;
  private final SelectionStrategy strategyUncovered;
  private final SelectionStrategy strategyCovered;

  public PreferReachesUncoveredStrategy(final GrammarGraph grammarGraph,
      final AlternativeCoverage coverage,
      final SelectionStrategy strategyUncovered, final SelectionStrategy strategyCovered) {
    this.reachableChoices = ReachableChoicesComputation.computeReachableChoices(grammarGraph);
    this.minHeights = MinHeightComputation.computeMinHeights(grammarGraph);
    this.coverage = coverage;
    this.strategyUncovered = strategyUncovered;
    this.strategyCovered = strategyCovered;
  }

  private final boolean reachesUncoveredAlternative(final Choice choice, final int maxHeight) {
    for (final Alternative alternative : choice.getSuccessorEdges()) {
      if (reachesUncoveredAlternative(alternative, maxHeight)) {
        return true;
      }
    }

    return false;
  }

  private final boolean reachesUncoveredAlternative(final Alternative alternative,
      final int maxHeight) {
    if (!this.coverage.isCovered(alternative)) {
      return true;
    }

    // alternative has already been covered, but it may lead to other, uncovered alternatives
    final Sequence sequence = alternative.getTarget();
    assert (this.reachableChoices.containsKey(sequence));

    final Map<Choice, Integer> reachableChoicesAlternative = this.reachableChoices.get(sequence);
    for (final Map.Entry<Choice, Integer> entry : reachableChoicesAlternative.entrySet()) {
      final Choice choice = entry.getKey();
      final int distance = entry.getValue();

      assert (this.minHeights.containsKey(choice));
      final int minHeightChoice = this.minHeights.get(choice);

      if (distance + minHeightChoice <= maxHeight) {
        for (final Alternative reachableAlternative : choice.getSuccessorEdges()) {
          if (!this.coverage.isCovered(reachableAlternative)) {
            return true;
          }
        }
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
  public final int chooseCount(final Element element, final int maxHeight) {
    if (reachesUncoveredAlternative(element.getTarget(), maxHeight)) {
      return this.strategyUncovered.chooseCount(element, maxHeight);
    } else {
      return this.strategyCovered.chooseCount(element, maxHeight);
    }
  }

}
