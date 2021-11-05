package i2.act.fuzzer.selection;

import i2.act.grammargraph.GrammarGraphEdge.Alternative;
import i2.act.grammargraph.GrammarGraphEdge.Element;

import java.util.Random;

public final class UniformRandomSelection extends RandomSelection {

  public UniformRandomSelection() {
    super();
  }

  public UniformRandomSelection(final long seed) {
    super(seed);
  }

  public UniformRandomSelection(final Random rng) {
    super(rng);
  }

  @Override
  protected final int getWeight(final Alternative alternative) {
    return 1;
  }

  @Override
  protected final int getWeight(final Element element) {
    return 1;
  }

}
