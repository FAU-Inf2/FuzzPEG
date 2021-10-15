package i2.act.fuzzer.selection;

import i2.act.coverage.AlternativeCoverage;
import i2.act.grammargraph.GrammarGraphEdge.Alternative;
import i2.act.grammargraph.GrammarGraphEdge.Element;

import java.util.List;
import java.util.stream.Collectors;

public final class PreferUncoveredStrategy implements SelectionStrategy {

  private final AlternativeCoverage coverage;
  private final SelectionStrategy strategyUncovered;
  private final SelectionStrategy strategyCovered;

  public PreferUncoveredStrategy(final AlternativeCoverage coverage,
      final SelectionStrategy strategyUncovered, final SelectionStrategy strategyCovered) {
    this.coverage = coverage;
    this.strategyUncovered = strategyUncovered;
    this.strategyCovered = strategyCovered;
  }

  @Override
  public final Alternative chooseAlternative(final List<Alternative> alternatives) {
    final List<Alternative> uncoveredAlternatives = alternatives.stream()
        .filter((alternative) -> !this.coverage.isCovered(alternative))
        .collect(Collectors.toList());

    if (uncoveredAlternatives.isEmpty()) {
      return this.strategyCovered.chooseAlternative(alternatives);
    } else {
      return this.strategyUncovered.chooseAlternative(uncoveredAlternatives);
    }
  }

  @Override
  public final int chooseCount(final Element element) {
    return this.strategyCovered.chooseCount(element);
  }

}
