package i2.act.fuzzer;

import i2.act.packrat.Lexer;
import i2.act.packrat.Token;
import i2.act.packrat.TokenStream;
import i2.act.packrat.cst.Node;
import i2.act.packrat.cst.TerminalNode;
import i2.act.packrat.cst.visitors.SyntaxTreeVisitor;
import i2.act.peg.ast.Grammar;
import i2.act.peg.symbols.LexerSymbol;

import java.util.List;

public final class TokenJoiner {

  private final Lexer lexer;
  private final String separator;

  public TokenJoiner(final Grammar grammar, final String separator) {
    this.lexer = Lexer.forGrammar(grammar);
    this.separator = separator;
  }

  public final String join(final List<Token> tokens) {
    if (tokens.isEmpty()) {
      return "";
    }

    final StringBuilder builder = new StringBuilder();

    Token lastToken = null;
    for (final Token token : tokens) {
      if (needsSeparator(lastToken, token)) {
        builder.append(this.separator);
      }

      builder.append(token.getValue());

      lastToken = token;
    }

    return builder.toString();
  }

  public final String join(final Node<?> syntaxTree) {
    final StringBuilder builder = new StringBuilder();

    syntaxTree.accept(new SyntaxTreeVisitor<Void, Void>() {

      private Token lastToken = null;

      @Override
      public final Void visit(final TerminalNode node, final Void parameter) {
        final Token token = node.getToken();

        if (needsSeparator(this.lastToken, token)) {
          builder.append(TokenJoiner.this.separator);
        }

        builder.append(token.getValue());

        this.lastToken = token;

        return null;
      }

    }, null);

    return builder.toString();
  }

  private final boolean needsSeparator(final Token firstToken, final Token secondToken) {
    if (firstToken == null) {
      return false;
    }

    if (secondToken.getTokenSymbol() == LexerSymbol.EOF) {
      return false;
    }

    if (firstToken.getTokenSymbol() == null && secondToken.getTokenSymbol() == null) {
      return true;
    }

    final TokenStream tokens;
    {
      final String joined = firstToken.getValue() + secondToken.getValue();

      try {
        tokens = this.lexer.lex(joined, true);
      } catch (final Exception exception) {
        return true;
      }
    }

    if (tokens.numberOfTokens() != 2) {
      return true;
    }

    final Token lexedFirstToken = tokens.at(0);
    final Token lexedLastToken = tokens.at(tokens.numberOfTokens() - 1);

    return lexedFirstToken.getTokenSymbol() != firstToken.getTokenSymbol()
        || lexedLastToken.getTokenSymbol() != secondToken.getTokenSymbol();
  }

}
