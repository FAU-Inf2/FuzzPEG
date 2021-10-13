package i2.act.fuzzer.selection;

import i2.act.grammargraph.GrammarGraph;
import i2.act.grammargraph.GrammarGraphEdge.Alternative;
import i2.act.grammargraph.GrammarGraphEdge.Element;
import i2.act.grammargraph.GrammarGraphEdge.Element.Quantifier;
import i2.act.grammargraph.GrammarGraphNode;
import i2.act.grammargraph.GrammarGraphNode.Sequence;
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
  public final Sequence chooseAlternative(final List<Alternative> alternatives) {
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

    return this.baseStrategy.chooseAlternative(remainingAlternatives);
  }

  @Override
  public final int chooseCount(final Element element) {
    final Quantifier quantifier = element.getQuantifier();

    if (quantifier == Quantifier.QUANT_OPTIONAL) {
      return (chooseSmall()) ? 0 : 1;
    } else {
      assert (quantifier == Quantifier.QUANT_STAR
          || quantifier == Quantifier.QUANT_PLUS);

      int count = (quantifier == Quantifier.QUANT_PLUS) ? 1 : 0;

      final int adjustedWeight = (int)((1.0 - this.probability) * (element.getWeight()));
      while (this.rng.nextInt(adjustedWeight + 1) != 0) {
        ++count;
      }

      return count;
    }
  }

  private final boolean chooseSmall() {
    return this.rng.nextDouble() < this.probability;
  }

}
