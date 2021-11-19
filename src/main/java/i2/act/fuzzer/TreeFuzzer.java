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
    return generate(this.grammarGraph.getRootNode(), this.maxHeight, null);
  }

  private final Node<?> generate(final Choice choice, final int maxHeight, final Node<?> parent) {
    if (isTerminal(choice)) {
      final Node<?> node = createTerminalNode(choice, parent);
      return (parent == null) ? (node) : (parent);
    }

    final Node<?> node = createNonTerminalNode(choice, parent);

    final int childHeight = childHeight(choice, maxHeight);
    final Alternative chosen =
        chooseAlternative(viableAlternatives(choice, childHeight), childHeight);

    track(chosen);

    for (final Element element : elementsOf(chosen)) {
      final Node<?> quantifierNode = createQuantifierNode(element, node);
      for (int count = 0; generateMoreElements(element, count, childHeight); ++count) {
        generate(element.getTarget(), childHeight, createItemNode(quantifierNode, element));
      }
    }

    return (parent == null) ? (node) : (parent);
  }

  private final Node<?> createTerminalNode(final Choice choice, final Node<?> parent) {
    assert (choice.hasGrammarSymbol());
    assert (choice.getGrammarSymbol() instanceof LexerSymbol);

    final LexerSymbol symbol = (LexerSymbol) choice.getGrammarSymbol();
    final Token token = this.tokenGenerator.createToken(symbol);

    final Node<?> node = new TerminalNode(token);

    if (parent != null) {
      parent.getChildren().add(node);
    }

    return node;
  }

  private final Node<?> createNonTerminalNode(final Choice choice, final Node<?> parent) {
    if (isProduction(choice)) {
      assert (choice.hasGrammarSymbol());
      assert (choice.getGrammarSymbol() instanceof ParserSymbol);

      final ParserSymbol symbol = (ParserSymbol) choice.getGrammarSymbol();

      final Node<?> node = new NonTerminalNode(symbol, new ArrayList<Node<?>>());

      if (parent != null) {
        parent.getChildren().add(node);
      }

      return node;
    } else {
      return parent;
    }
  }

  private final Node<?> createItemNode(final Node<?> parent, final Element element) {
    if (parent != null
        && (parent instanceof NonTerminalNode) && ((NonTerminalNode) parent).isQuantifierNode()) {
      final NonTerminalNode itemNode =
          new NonTerminalNode(ParserSymbol.LIST_ITEM, new ArrayList<>());

      itemNode.setExpectedSymbol(element.getTarget().getGrammarSymbol());

      parent.getChildren().add(itemNode);

      return itemNode;
    } else {
      return parent;
    }
  }

  private final Node<?> createQuantifierNode(final Element element, final Node<?> node) {
    if (element.getQuantifier() == Element.Quantifier.QUANT_NONE) {
      return node;
    } else {
      final ParserSymbol quantifierSymbol;
      {
        switch (element.getQuantifier()) {
          case QUANT_OPTIONAL: {
            quantifierSymbol = ParserSymbol.OPTIONAL;
            break;
          }
          case QUANT_STAR: {
            quantifierSymbol = ParserSymbol.STAR;
            break;
          }
          case QUANT_PLUS: {
            quantifierSymbol = ParserSymbol.PLUS;
            break;
          }
          default: {
            assert (false);
            throw new RuntimeException("unknown quantifier: " + element.getQuantifier());
          }
        }
      }

      final NonTerminalNode quantifierNode =
          new NonTerminalNode(quantifierSymbol, new ArrayList<Node<?>>());

      node.getChildren().add(quantifierNode);
      return quantifierNode;
    }
  }

}
