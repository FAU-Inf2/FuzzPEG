package i2.act.fuzzer;

import i2.act.grammargraph.GrammarGraph;
import i2.act.grammargraph.GrammarGraphEdge.Alternative;
import i2.act.grammargraph.GrammarGraphEdge.Element;
import i2.act.grammargraph.GrammarGraphEdge.Element.Quantifier;
import i2.act.grammargraph.GrammarGraphNode;
import i2.act.grammargraph.GrammarGraphNode.Choice;
import i2.act.grammargraph.GrammarGraphNode.Sequence;
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
  private final TokenJoiner joiner;

  private final Map<GrammarGraphNode<?,?>, Integer> minHeights;
  private final int minMaxHeight;

  private final TokenGenerator tokenGenerator;
  private final SelectionStrategy selectionStrategy;

  public Fuzzer(final GrammarGraph grammarGraph, final TokenJoiner joiner,
      final TokenGenerator tokenGenerator, final SelectionStrategy selectionStrategy) {
    this.grammarGraph = grammarGraph;
    this.joiner = joiner;

    this.minHeights = MinHeightComputation.computeMinHeights(grammarGraph);

    assert (this.minHeights.containsKey(grammarGraph.getRootNode()));
    this.minMaxHeight = this.minHeights.get(grammarGraph.getRootNode());

    this.tokenGenerator = tokenGenerator;
    this.selectionStrategy = selectionStrategy;
  }

  public final String generateProgram(final int maxHeight) {
    final Node<?> tree = generateTree(maxHeight);
    return this.joiner.join(tree);
  }

  public final Node<?> generateTree(final int maxHeight) {
    if (maxHeight < this.minMaxHeight) {
      throw new RuntimeException(String.format(
          "maxHeight too small (the given grammar requires a maxHeight of at least %d)",
          this.minMaxHeight));
    }

    final List<Node<?>> nodes = generate(this.grammarGraph.getRootNode(), maxHeight);

    assert (nodes.size() == 1);
    return nodes.get(0);
  }

  public final List<Node<?>> generate(final Choice choice, final int maxHeight) {
    if (isTerminal(choice)) {
      return listOf(createTerminalNode(choice));
    }

    final int childHeight = childHeight(choice, maxHeight);
    final Sequence chosen = chooseAlternative(viableAlternatives(choice, childHeight));

    final List<Node<?>> children = new ArrayList<>();

    for (final Element element : chosen.getSuccessorEdges()) {
      for (int count = count(element, childHeight); count-- > 0;) {
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

  private final List<Alternative> viableAlternatives(final Choice choice, final int maxHeight) {
    final List<Alternative> viableAlternatives = new ArrayList<>();

    for (final Alternative alternative : choice.getSuccessorEdges()) {
      assert (this.minHeights.containsKey(alternative.getTarget()));
      if (this.minHeights.get(alternative.getTarget()) <= maxHeight) {
        viableAlternatives.add(alternative);
      }
    }

    assert (!viableAlternatives.isEmpty());
    return viableAlternatives;
  }

  private final Sequence chooseAlternative(final List<Alternative> alternatives) {
    return this.selectionStrategy.chooseAlternative(alternatives);
  }

  private final int count(final Element element, final int maxHeight) {
    final Quantifier quantifier = element.getQuantifier();

    assert (this.minHeights.containsKey(element.getTarget()));
    if (this.minHeights.get(element.getTarget()) > maxHeight) {
      assert (quantifier != Quantifier.QUANT_NONE
          && quantifier != Quantifier.QUANT_PLUS);
      return 0;
    }

    if (quantifier == Quantifier.QUANT_NONE) {
      return 1;
    }

    return this.selectionStrategy.chooseCount(element);
  }

  private final int childHeight(final Choice choice, final int maxHeight) {
    return (isProduction(choice)) ? (maxHeight - 1) : (maxHeight);
  }

  private final List<Node<?>> listOf(final Node<?> node) {
    return Arrays.asList(node);
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
