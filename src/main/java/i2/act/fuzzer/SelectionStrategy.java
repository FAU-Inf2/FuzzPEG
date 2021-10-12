package i2.act.fuzzer;

import i2.act.grammargraph.GrammarGraphEdge.AlternativeEdge;
import i2.act.grammargraph.GrammarGraphEdge.SequenceEdge;
import i2.act.grammargraph.GrammarGraphNode.SequenceNode;

import java.util.List;

public interface SelectionStrategy {

  public abstract SequenceNode chooseAlternative(final List<AlternativeEdge> alternatives);

  public abstract int chooseCount(final SequenceEdge element);

}
