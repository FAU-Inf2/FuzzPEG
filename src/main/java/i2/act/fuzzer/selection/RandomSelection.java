package i2.act.fuzzer.selection;

import i2.act.grammargraph.GrammarGraphEdge.Alternative;
import i2.act.grammargraph.GrammarGraphEdge.Element;
import i2.act.util.RandomNumberGenerator;

import java.util.List;

public abstract class RandomSelection implements SelectionStrategy {

  private final RandomNumberGenerator rng;

  public RandomSelection() {
    this(System.currentTimeMillis());
  }

  public RandomSelection(final long seed) {
    this(new RandomNumberGenerator(seed));
  }

  public RandomSelection(final RandomNumberGenerator rng) {
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
        .map(alternative -> getWeight(alternative))
        .reduce(0, Integer::sum);

    final int chosen = this.rng.nextInt(totalWeight + 1);
    int weightSum = 0;

    for (final Alternative alternative : alternatives) {
      weightSum += getWeight(alternative);

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
    return this.rng.nextInt(getWeight(element) + 1) != 0;
  }

  protected abstract int getWeight(final Alternative alternative);

  protected abstract int getWeight(final Element element);

}
