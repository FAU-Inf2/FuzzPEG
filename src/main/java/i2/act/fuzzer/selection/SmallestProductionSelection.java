package i2.act.fuzzer.selection;

import i2.act.grammargraph.GrammarGraph;
import i2.act.grammargraph.GrammarGraphEdge.Alternative;
import i2.act.grammargraph.GrammarGraphEdge.Element;
import i2.act.grammargraph.GrammarGraphNode;
import i2.act.grammargraph.properties.MinSizeComputation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class SmallestProductionSelection implements SelectionStrategy {

  private final SelectionStrategy baseStrategy;
  private final double probability;
  private final Random rng;

  private final Map<GrammarGraphNode<?,?>, Integer> minSizes;

  public SmallestProductionSelection(final GrammarGraph grammarGraph,
      final SelectionStrategy baseStrategy, final double probability) {
    this(grammarGraph, baseStrategy, probability, System.currentTimeMillis());
  }

  public SmallestProductionSelection(final GrammarGraph grammarGraph,
      final SelectionStrategy baseStrategy, final double probability, final long seed) {
    this(grammarGraph, baseStrategy, probability, new Random(seed));
  }

  public SmallestProductionSelection(final GrammarGraph grammarGraph,
      final SelectionStrategy baseStrategy, final double probability, final Random rng) {
    this.minSizes = MinSizeComputation.computeMinSizes(grammarGraph);

    this.baseStrategy = baseStrategy;
    this.probability = probability;
    this.rng = rng;
  }

  @Override
  public final Alternative chooseAlternative(final List<Alternative> alternatives,
      final int maxHeight) {
    assert (!alternatives.isEmpty());

    final List<Alternative> remainingAlternatives;
    {
      if (chooseSmall()) {
        remainingAlternatives = new ArrayList<>();
        int minSize = Integer.MAX_VALUE;

        for (final Alternative alternative : alternatives) {
          assert (this.minSizes.containsKey(alternative.getTarget()));
          final int minSizeAlternative = this.minSizes.get(alternative.getTarget());

          if (minSizeAlternative < minSize) {
            minSize = minSizeAlternative;
            remainingAlternatives.clear();
          }

          if (minSizeAlternative == minSize) {
            remainingAlternatives.add(alternative);
          }
        }
      } else {
        remainingAlternatives = alternatives;
      }
    }

    return this.baseStrategy.chooseAlternative(remainingAlternatives, maxHeight);
  }

  @Override
  public final boolean generateMoreElements(final Element element, final int count,
      final int maxHeight) {
    final double adjustedWeight = (1.0 - this.probability) * (element.getWeight());
    return this.rng.nextDouble() * (adjustedWeight + 1) > 1.0;
  }

  private final boolean chooseSmall() {
    return this.rng.nextDouble() < this.probability;
  }

}
