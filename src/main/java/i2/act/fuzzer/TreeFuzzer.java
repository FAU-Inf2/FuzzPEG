package i2.act.fuzzer;

import i2.act.coverage.AlternativeCoverage;
import i2.act.fuzzer.selection.SelectionStrategy;
import i2.act.fuzzer.tokens.TokenGenerator;
import i2.act.grammargraph.GrammarGraph;
import i2.act.grammargraph.GrammarGraphEdge.Alternative;
import i2.act.grammargraph.GrammarGraphEdge.Element;
import i2.act.grammargraph.GrammarGraphNode.Choice;
import i2.act.packrat.Token;
import i2.act.packrat.cst.Node;
import i2.act.packrat.cst.NonTerminalNode;
import i2.act.packrat.cst.TerminalNode;
import i2.act.peg.symbols.LexerSymbol;
import i2.act.peg.symbols.ParserSymbol;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class TreeFuzzer extends Fuzzer<Node<?>> {

  public TreeFuzzer(final GrammarGraph grammarGraph, final int maxHeight,
      final TokenGenerator tokenGenerator, final SelectionStrategy selectionStrategy) {
    this(grammarGraph, maxHeight, tokenGenerator, selectionStrategy, null);
  }

  public TreeFuzzer(final GrammarGraph grammarGraph, final int maxHeight,
      final TokenGenerator tokenGenerator, final SelectionStrategy selectionStrategy,
      final AlternativeCoverage coverage) {
    super(grammarGraph, maxHeight, tokenGenerator, selectionStrategy, coverage);
  }

  @Override
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

  private final List<Node<?>> listOf(final Node<?> node) {
    return Arrays.asList(node);
  }

}
