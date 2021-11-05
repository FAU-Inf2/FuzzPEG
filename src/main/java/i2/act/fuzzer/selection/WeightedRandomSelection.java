package i2.act.fuzzer.selection;

import i2.act.grammargraph.GrammarGraphEdge.Alternative;
import i2.act.grammargraph.GrammarGraphEdge.Element;

import java.util.Random;

public final class WeightedRandomSelection extends RandomSelection {

  public WeightedRandomSelection() {
    super();
  }

  public WeightedRandomSelection(final long seed) {
    super(seed);
  }

  public WeightedRandomSelection(final Random rng) {
    super(rng);
  }

  @Override
  protected final int getWeight(final Alternative alternative) {
    return alternative.getWeight();
  }

  @Override
  protected final int getWeight(final Element element) {
    return element.getWeight();
  }

}
