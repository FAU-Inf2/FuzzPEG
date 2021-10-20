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
import i2.act.packrat.Token;
import i2.act.packrat.cst.Node;
import i2.act.packrat.cst.NonTerminalNode;
import i2.act.packrat.cst.TerminalNode;
import i2.act.peg.symbols.LexerSymbol;
import i2.act.peg.symbols.ParserSymbol;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class Fuzzer {

  private final GrammarGraph grammarGraph;
  private final int maxHeight;

  private final Map<GrammarGraphNode<?,?>, Integer> minHeights;
  private final int minMaxHeight;

  private final TokenGenerator tokenGenerator;
  private final SelectionStrategy selectionStrategy;

  private final AlternativeCoverage coverage;

  public Fuzzer(final GrammarGraph grammarGraph, final int maxHeight,
      final TokenGenerator tokenGenerator, final SelectionStrategy selectionStrategy) {
    this(grammarGraph, maxHeight, tokenGenerator, selectionStrategy, null);
  }

  public Fuzzer(final GrammarGraph grammarGraph, final int maxHeight,
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

  public final Node<?> generate() {
    final List<Node<?>> nodes = generate(this.grammarGraph.getRootNode(), this.maxHeight);

    assert (nodes.size() == 1);
    return nodes.get(0);
  }

  private final List<Node<?>> generate(final Choice choice, final int maxHeight) {
    if (isTerminal(choice)) {
      return listOf(createTerminalNode(choice));
    }

    final int childHeight = childHeight(choice, maxHeight);
    final Alternative chosen =
        chooseAlternative(viableAlternatives(choice, childHeight), childHeight);

    track(chosen);

    final List<Node<?>> children = new ArrayList<>();

    for (final Element element : elementsOf(chosen)) {
      for (int count = 0; generateMoreElements(element, count, childHeight); ++count) {
        children.addAll(generate(element.getTarget(), childHeight));
      }
    }

    if (isProduction(choice)) {
      return listOf(createNonTerminalNode(choice, children));
    } else {
      return children;
    }
  }

  private final boolean isTerminal(final Choice choice) {
    return choice.getGrammarSymbol() instanceof LexerSymbol;
  }

  private final List<Alternative> viableAlternatives(final Choice choice, final int childHeight) {
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

  private final Alternative chooseAlternative(final List<Alternative> alternatives,
      final int childHeight) {
    return this.selectionStrategy.chooseAlternative(alternatives, childHeight);
  }

  private final void track(final Alternative chosen) {
    if (this.coverage != null) {
      this.coverage.covered(chosen);
    }
  }

  private final boolean generateMoreElements(final Element element, final int count,
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

  private final int childHeight(final Choice choice, final int maxHeight) {
    return (isProduction(choice)) ? (maxHeight - 1) : (maxHeight);
  }

  private final List<Node<?>> listOf(final Node<?> node) {
    return Arrays.asList(node);
  }

  private final List<Element> elementsOf(final Alternative alternative) {
    return alternative.getTarget().getSuccessorEdges();
  }

  private final boolean isProduction(final Choice choice) {
    return choice.hasGrammarSymbol() && choice.getGrammarSymbol().getProduction() != null;
  }

  private final TerminalNode createTerminalNode(final Choice choice) {
    assert (choice.hasGrammarSymbol());
    assert (choice.getGrammarSymbol() instanceof LexerSymbol);

    final LexerSymbol symbol = (LexerSymbol) choice.getGrammarSymbol();
    final Token token = this.tokenGenerator.createToken(symbol);

    return new TerminalNode(token);
  }

  private final NonTerminalNode createNonTerminalNode(final Choice choice,
      final List<Node<?>> children) {
    assert (choice.hasGrammarSymbol());
    assert (choice.getGrammarSymbol() instanceof ParserSymbol);

    final ParserSymbol symbol = (ParserSymbol) choice.getGrammarSymbol();

    return new NonTerminalNode(symbol, children);
  }

}
