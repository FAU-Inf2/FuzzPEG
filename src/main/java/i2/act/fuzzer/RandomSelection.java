package i2.act.fuzzer;

import i2.act.grammargraph.GrammarGraphEdge.AlternativeEdge;
import i2.act.grammargraph.GrammarGraphEdge.SequenceEdge;
import i2.act.grammargraph.GrammarGraphNode.SequenceNode;

import java.util.List;
import java.util.Random;

public final class RandomSelection implements SelectionStrategy {

  private final Random rng;

  public RandomSelection() {
    this(System.currentTimeMillis());
  }

  public RandomSelection(final long seed) {
    this(new Random(seed));
  }

  public RandomSelection(final Random rng) {
    this.rng = rng;
  }

  public final void setSeed(final long seed) {
    this.rng.setSeed(seed);
  }

  @Override
  public final SequenceNode chooseAlternative(final List<AlternativeEdge> alternatives) {
    assert (!alternatives.isEmpty());

    // handle fast case first
    if (alternatives.size() == 1) {
      return alternatives.get(0).getTarget();
    }

    // roulette wheel selection
    final int totalWeight = alternatives.stream()
        .map(AlternativeEdge::getWeight)
        .reduce(0, Integer::sum);

    final int chosen = this.rng.nextInt(totalWeight) + 1;
    int weightSum = 0;

    for (final AlternativeEdge alternative : alternatives) {
      weightSum += alternative.getWeight();

      if (weightSum >= chosen) {
        return alternative.getTarget();
      }
    }

    assert (false);
    return null;
  }

  @Override
  public final int chooseCount(final SequenceEdge element) {
    final SequenceEdge.Quantifier quantifier = element.getQuantifier();

    if (quantifier == SequenceEdge.Quantifier.QUANT_OPTIONAL) {
      return (this.rng.nextInt(element.getWeight() + 1) == 0) ? 0 : 1;
    } else {
      assert (quantifier == SequenceEdge.Quantifier.QUANT_STAR
          || quantifier == SequenceEdge.Quantifier.QUANT_PLUS);

      int count = (quantifier == SequenceEdge.Quantifier.QUANT_PLUS) ? 1 : 0;
      while (this.rng.nextInt(element.getWeight() + 1) != 0) {
        ++count;
      }

      return count;
    }
  }

}
