package i2.act.fuzzer;

import i2.act.coverage.AlternativeCoverage;
import i2.act.fuzzer.selection.SelectionStrategy;
import i2.act.fuzzer.tokens.TokenGenerator;
import i2.act.grammargraph.GrammarGraph;
import i2.act.grammargraph.GrammarGraphEdge.Alternative;
import i2.act.grammargraph.GrammarGraphEdge.Element;
import i2.act.grammargraph.GrammarGraphEdge.Element.Quantifier;
import i2.act.grammargraph.GrammarGraphNode;
import i2.act.grammargraph.GrammarGraphNode.Choice;
import i2.act.grammargraph.properties.MinHeightComputation;
import i2.act.peg.symbols.LexerSymbol;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class Fuzzer<R> {

  protected final GrammarGraph grammarGraph;
  protected final int maxHeight;

  protected final Map<GrammarGraphNode<?,?>, Integer> minHeights;
  protected final int minMaxHeight;

  protected final TokenGenerator tokenGenerator;
  protected final SelectionStrategy selectionStrategy;

  protected final AlternativeCoverage coverage;

  protected Fuzzer(final GrammarGraph grammarGraph, final int maxHeight,
      final TokenGenerator tokenGenerator, final SelectionStrategy selectionStrategy,
      final AlternativeCoverage coverage) {
    this.grammarGraph = grammarGraph;
    this.maxHeight = maxHeight;

    this.tokenGenerator = tokenGenerator;
    this.selectionStrategy = selectionStrategy;

    this.coverage = coverage;

    this.minHeights = MinHeightComputation.computeMinHeights(grammarGraph);

    assert (this.minHeights.containsKey(grammarGraph.getRootNode()));
    this.minMaxHeight = this.minHeights.get(grammarGraph.getRootNode());

    if (this.maxHeight < this.minMaxHeight) {
      throw new RuntimeException(String.format(
          "maxHeight too small (the given grammar requires a maxHeight of at least %d)",
          this.minMaxHeight));
    }
  }

  public abstract R generate();

  protected final boolean isTerminal(final Choice choice) {
    return choice.getGrammarSymbol() instanceof LexerSymbol;
  }

  protected final List<Alternative> viableAlternatives(final Choice choice, final int childHeight) {
    final List<Alternative> viableAlternatives = new ArrayList<>();

    for (final Alternative alternative : choice.getSuccessorEdges()) {
      assert (this.minHeights.containsKey(alternative.getTarget()));
      if (this.minHeights.get(alternative.getTarget()) <= childHeight) {
        viableAlternatives.add(alternative);
      }
    }

    assert (!viableAlternatives.isEmpty());
    return viableAlternatives;
  }

  protected final Alternative chooseAlternative(final List<Alternative> alternatives,
      final int childHeight) {
    return this.selectionStrategy.chooseAlternative(alternatives, childHeight);
  }

  protected final void track(final Alternative chosen) {
    if (this.coverage != null) {
      this.coverage.covered(chosen);
    }
  }

  protected final boolean generateMoreElements(final Element element, final int count,
      final int childHeight) {
    final Quantifier quantifier = element.getQuantifier();

    assert (this.minHeights.containsKey(element.getTarget()));
    if (this.minHeights.get(element.getTarget()) > childHeight) {
      assert (quantifier != Quantifier.QUANT_NONE
          && quantifier != Quantifier.QUANT_PLUS);
      assert (count == 0);
      return false;
    }

    if (quantifier == Quantifier.QUANT_NONE) {
      if (count == 0) {
        return true;
      } else {
        assert (count == 1);
        return false;
      }
    }

    if (quantifier == Quantifier.QUANT_PLUS && count == 0) {
      return true;
    }

    if (quantifier == Quantifier.QUANT_OPTIONAL && count == 1) {
      return false;
    }

    return this.selectionStrategy.generateMoreElements(element, count, childHeight);
  }

  protected final int childHeight(final Choice choice, final int maxHeight) {
    return (isProduction(choice)) ? (maxHeight - 1) : (maxHeight);
  }

  protected final List<Element> elementsOf(final Alternative alternative) {
    return alternative.getTarget().getSuccessorEdges();
  }

  protected final boolean isProduction(final Choice choice) {
    return choice.hasGrammarSymbol() && choice.getGrammarSymbol().getProduction() != null;
  }

}
