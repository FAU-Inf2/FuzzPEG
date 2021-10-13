package i2.act.fuzzer.selection;

import i2.act.grammargraph.GrammarGraphEdge.Alternative;
import i2.act.grammargraph.GrammarGraphEdge.Element;

import java.util.List;

public interface SelectionStrategy {

  public abstract Alternative chooseAlternative(final List<Alternative> alternatives);

  public abstract int chooseCount(final Element element);

}
