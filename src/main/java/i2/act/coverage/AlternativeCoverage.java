package i2.act.coverage;

import i2.act.grammargraph.GrammarGraph;
import i2.act.grammargraph.GrammarGraphEdge.Alternative;
import i2.act.grammargraph.GrammarGraphNode;
import i2.act.grammargraph.GrammarGraphNode.Choice;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public final class AlternativeCoverage {

  private final Set<Alternative> allAlternatives;

  private final Set<Alternative> covered;

  public AlternativeCoverage(final GrammarGraph grammarGraph) {
    this.allAlternatives = determineAllAlternatives(grammarGraph);
    this.covered = new HashSet<Alternative>();
  }

  private final Set<Alternative> determineAllAlternatives(final GrammarGraph grammarGraph) {
    return StreamSupport.stream(grammarGraph.spliterator(), false)
        .filter(Choice.class::isInstance)
        .map(Choice.class::cast)
        .map(GrammarGraphNode::getSuccessorEdges)
        .flatMap(List::stream)
        .collect(Collectors.toSet());
  }

  public final void covered(final Alternative alternative) {
    this.covered.add(alternative);
  }

  public final boolean isCovered(final Alternative alternative) {
    return this.covered.contains(alternative);
  }

  public final int totalCount() {
    return this.allAlternatives.size();
  }

  public final int coveredCount() {
    return this.covered.size();
  }

  public final int missingCount() {
    return totalCount() - coveredCount();
  }

  public final boolean isFullyCovered() {
    return missingCount() == 0;
  }

}
