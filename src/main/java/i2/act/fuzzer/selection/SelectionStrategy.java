package i2.act.fuzzer.selection;

import i2.act.grammargraph.GrammarGraphEdge.Alternative;
import i2.act.grammargraph.GrammarGraphEdge.Element;

import java.util.List;

public interface SelectionStrategy {

  public abstract Alternative chooseAlternative(final List<Alternative> alternatives,
      final int maxHeight);

  // TODO remove
  public abstract int chooseCount(final Element element, final int maxHeight);

  public abstract boolean generateMoreElements(final Element element, final int count,
      final int maxHeight);

}
