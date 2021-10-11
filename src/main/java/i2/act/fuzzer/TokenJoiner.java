package i2.act.fuzzer;

import i2.act.packrat.Lexer;
import i2.act.packrat.Token;
import i2.act.packrat.TokenStream;
import i2.act.packrat.cst.Node;
import i2.act.packrat.cst.TerminalNode;
import i2.act.packrat.cst.visitors.SyntaxTreeVisitor;
import i2.act.packrat.nfa.NFA;
import i2.act.packrat.nfa.NFAState;
import i2.act.packrat.nfa.Transition;
import i2.act.peg.ast.Grammar;
import i2.act.peg.symbols.LexerSymbol;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TokenJoiner {

  private final Lexer lexer;
  private final String separator;

  private final Map<LexerSymbol, Map<LexerSymbol, Boolean>> needsSeparator;

  public TokenJoiner(final Grammar grammar, final String separator) {
    this.lexer = Lexer.forGrammar(grammar);
    this.separator = separator;

    this.needsSeparator = new HashMap<LexerSymbol, Map<LexerSymbol, Boolean>>();
    determineSeparators(grammar);
  }

  private final void determineSeparators(final Grammar grammar) {
    for (final LexerSymbol firstSymbol : grammar.getLexerSymbols()) {
      final Map<LexerSymbol, Boolean> firstMap = new HashMap<>();
      this.needsSeparator.put(firstSymbol, firstMap);

      final NFA firstNFA = this.lexer.getNFA(firstSymbol);
      assert (firstNFA != null);

      if (firstNFA.hasLiteralString()) {
        final String firstLiteral = firstNFA.getLiteralString();
        boolean allOthersDoNotMatch = true;

        for (final LexerSymbol secondSymbol : grammar.getLexerSymbols()) {
          if (firstSymbol == secondSymbol) {
            continue;
          }

          final NFA secondNFA = this.lexer.getNFA(secondSymbol);
          if (secondNFA.isPossiblePrefix(firstLiteral)) {
            allOthersDoNotMatch = false;
            break;
          }
        }

        if (allOthersDoNotMatch) {
          for (final LexerSymbol secondSymbol : grammar.getLexerSymbols()) {
            firstMap.put(secondSymbol, false);
          }
        } else {
          for (final LexerSymbol secondSymbol : grammar.getLexerSymbols()) {
            if (firstSymbol == secondSymbol || secondSymbol.isSkippedToken()) {
              continue;
            }

            final NFA secondNFA = this.lexer.getNFA(secondSymbol);
            if (secondNFA.isPossiblePrefix(firstLiteral)) {
              firstMap.put(secondSymbol, true);
            }
          }
        }
      }

      for (final LexerSymbol secondSymbol : grammar.getLexerSymbols()) {
        if (firstMap.containsKey(secondSymbol)) {
          // already handled above
          continue;
        }

        final NFA secondNFA = this.lexer.getNFA(secondSymbol);
        assert (secondNFA != null);

        if (firstNFA.hasLiteralString() && secondNFA.hasLiteralString()) {
          final String joined = firstNFA.getLiteralString() + secondNFA.getLiteralString();
          final boolean needsSeparator = needsSeparator(joined, firstSymbol, secondSymbol);

          firstMap.put(secondSymbol, needsSeparator);
        }
      }
    }

    for (final LexerSymbol secondSymbol : grammar.getLexerSymbols()) {
      final NFA secondNFA = this.lexer.getNFA(secondSymbol);

      if (!secondNFA.hasLiteralString()) {
        continue;
      }

      final String secondLiteral = secondNFA.getLiteralString();
      boolean allOthersDoNotMatch = true;

      firstSymbol: for (final LexerSymbol firstSymbol : grammar.getLexerSymbols()) {
        if (firstSymbol == secondSymbol || firstSymbol.isSkippedToken()) {
          continue;
        }

        final NFA firstNFA = this.lexer.getNFA(firstSymbol);

        for (final char character : secondLiteral.toCharArray()) {
          if (canMatchCharacter(firstNFA, character)) {
            allOthersDoNotMatch = false;
            break firstSymbol;
          }
        }
      }

      if (allOthersDoNotMatch) {
        for (final LexerSymbol firstSymbol : grammar.getLexerSymbols()) {
          if (!this.needsSeparator.get(firstSymbol).containsKey(secondSymbol)) {
            this.needsSeparator.get(firstSymbol).put(secondSymbol, false);
          }
        }
      }
    }
  }

  private final boolean canMatchCharacter(final NFA nfa, final char character) {
    final Set<NFAState> visitedStates = new HashSet<>();
    return canMatchCharacter(nfa.getStartState(), character, visitedStates);
  }

  private final boolean canMatchCharacter(final NFAState state, final char character,
      final Set<NFAState> visitedStates) {
    visitedStates.add(state);

    for (final Transition transition : state.getTransitions()) {
      if (!transition.isEpsilonTransition() && transition.matches(character)) {
        return true;
      }

      final NFAState targetState = transition.getTo();

      if (!visitedStates.contains(targetState)
          && canMatchCharacter(targetState, character, visitedStates)) {
        return true;
      }
    }

    return false;
  }

  private final boolean needsSeparator(final Token firstToken, final Token secondToken) {
    if (firstToken == null) {
      return false;
    }

    final LexerSymbol firstSymbol = firstToken.getTokenSymbol();
    final LexerSymbol secondSymbol = secondToken.getTokenSymbol();

    if (secondSymbol == LexerSymbol.EOF) {
      return false;
    }

    if (firstSymbol == null && secondSymbol == null) {
      return true;
    }

    if (this.needsSeparator.get(firstSymbol).containsKey(secondSymbol)) {
      return this.needsSeparator.get(firstSymbol).get(secondSymbol);
    }

    final String joined = firstToken.getValue() + secondToken.getValue();
    return needsSeparator(joined, firstToken.getTokenSymbol(), secondToken.getTokenSymbol());
  }

  private final boolean needsSeparator(final String string, final LexerSymbol firstSymbol,
      final LexerSymbol secondSymbol) {
    final TokenStream tokens;
    {
      try {
        tokens = this.lexer.lex(string, true);
      } catch (final Exception exception) {
        return true;
      }
    }

    if (tokens.numberOfTokens() != 2) {
      return true;
    }

    final Token lexedFirstToken = tokens.at(0);
    final Token lexedLastToken = tokens.at(tokens.numberOfTokens() - 1);

    return lexedFirstToken.getTokenSymbol() != firstSymbol
        || lexedLastToken.getTokenSymbol() != secondSymbol;
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

}
