package i2.act.fuzzer;

import i2.act.grammargraph.GrammarGraphEdge.Alternative;
import i2.act.grammargraph.GrammarGraphEdge.Element;
import i2.act.grammargraph.GrammarGraphNode.Sequence;

import java.util.List;

public interface SelectionStrategy {

  public abstract Sequence chooseAlternative(final List<Alternative> alternatives);

  public abstract int chooseCount(final Element element);

}
