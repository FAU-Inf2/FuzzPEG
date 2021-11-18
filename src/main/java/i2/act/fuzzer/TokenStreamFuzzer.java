package i2.act.fuzzer;

import i2.act.coverage.AlternativeCoverage;
import i2.act.fuzzer.selection.SelectionStrategy;
import i2.act.fuzzer.tokens.TokenGenerator;
import i2.act.grammargraph.GrammarGraph;
import i2.act.grammargraph.GrammarGraphEdge.Alternative;
import i2.act.grammargraph.GrammarGraphEdge.Element;
import i2.act.grammargraph.GrammarGraphNode.Choice;
import i2.act.packrat.Token;
import i2.act.peg.symbols.LexerSymbol;

import java.util.ArrayList;
import java.util.List;

public final class TokenStreamFuzzer extends Fuzzer<List<Token>> {

  public TokenStreamFuzzer(final GrammarGraph grammarGraph, final int maxHeight,
      final TokenGenerator tokenGenerator, final SelectionStrategy selectionStrategy) {
    this(grammarGraph, maxHeight, tokenGenerator, selectionStrategy, null);
  }

  public TokenStreamFuzzer(final GrammarGraph grammarGraph, final int maxHeight,
      final TokenGenerator tokenGenerator, final SelectionStrategy selectionStrategy,
      final AlternativeCoverage coverage) {
    super(grammarGraph, maxHeight, tokenGenerator, selectionStrategy, coverage);
  }

  @Override
  public final List<Token> generate() {
    return generate(this.grammarGraph.getRootNode(), this.maxHeight, new ArrayList<Token>());
  }

  private final List<Token> generate(final Choice choice, final int maxHeight,
      final List<Token> tokens) {
    if (isTerminal(choice)) {
      tokens.add(createToken(choice));
      return tokens;
    }

    final int childHeight = childHeight(choice, maxHeight);
    final Alternative chosen =
        chooseAlternative(viableAlternatives(choice, childHeight), childHeight);

    track(chosen);

    for (final Element element : elementsOf(chosen)) {
      for (int count = 0; generateMoreElements(element, count, childHeight); ++count) {
        generate(element.getTarget(), childHeight, tokens);
      }
    }

    return tokens;
  }

  private final Token createToken(final Choice choice) {
    assert (choice.hasGrammarSymbol());
    assert (choice.getGrammarSymbol() instanceof LexerSymbol);

    final LexerSymbol symbol = (LexerSymbol) choice.getGrammarSymbol();
    return this.tokenGenerator.createToken(symbol);
  }

}
