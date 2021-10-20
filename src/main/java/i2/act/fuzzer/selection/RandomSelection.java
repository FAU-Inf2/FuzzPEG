package i2.act.fuzzer.selection;

import i2.act.grammargraph.GrammarGraphEdge.Alternative;
import i2.act.grammargraph.GrammarGraphEdge.Element;

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
  public final Alternative chooseAlternative(final List<Alternative> alternatives,
      final int maxHeight) {
    assert (!alternatives.isEmpty());

    // handle fast case first
    if (alternatives.size() == 1) {
      return alternatives.get(0);
    }

    // roulette wheel selection
    final int totalWeight = alternatives.stream()
        .map(Alternative::getWeight)
        .reduce(0, Integer::sum);

    final int chosen = this.rng.nextInt(totalWeight) + 1;
    int weightSum = 0;

    for (final Alternative alternative : alternatives) {
      weightSum += alternative.getWeight();

      if (weightSum >= chosen) {
        return alternative;
      }
    }

    assert (false);
    return null;
  }

  @Override
  public final boolean generateMoreElements(final Element element, final int count,
      final int maxHeight) {
    return this.rng.nextInt(element.getWeight() + 1) != 0;
  }

}
