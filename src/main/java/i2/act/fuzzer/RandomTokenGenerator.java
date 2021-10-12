package i2.act.fuzzer;

import i2.act.grammargraph.GrammarGraph;
import i2.act.packrat.Lexer;
import i2.act.packrat.Token;
import i2.act.packrat.TokenStream;
import i2.act.packrat.nfa.CharacterSet;
import i2.act.packrat.nfa.NFA;
import i2.act.packrat.nfa.NFAState;
import i2.act.packrat.nfa.Transition;
import i2.act.peg.ast.CharacterRange;
import i2.act.peg.ast.Group;
import i2.act.peg.ast.Range;
import i2.act.peg.ast.SingleCharacter;
import i2.act.peg.symbols.LexerSymbol;

import java.util.List;
import java.util.Random;

public final class RandomTokenGenerator implements TokenGenerator {

  private final Lexer lexer;
  private final Random rng;

  private final boolean checkTokens = true; // TODO make configurable

  public RandomTokenGenerator(final GrammarGraph grammarGraph, final Random rng) {
    this.lexer = Lexer.forGrammar(grammarGraph.getGrammar());
    this.rng = rng;
  }

  @Override
  public final Token createToken(final LexerSymbol lexerSymbol) {
    if (lexerSymbol == LexerSymbol.EOF) {
      return new Token(lexerSymbol, "");
    }

    final NFA nfa = this.lexer.getNFA(lexerSymbol);
    assert (nfa != null);

    String tokenValue;
    {
      if (nfa.hasLiteralString()) {
        tokenValue = nfa.getLiteralString();
      } else {
        do {
          final StringBuilder builder = new StringBuilder();
          createRandomString(nfa, builder);
          tokenValue = builder.toString();
        } while (this.checkTokens && !isValid(tokenValue, lexerSymbol));
      }
    }

    return new Token(lexerSymbol, tokenValue);
  }

  private final void createRandomString(final NFA nfa, final StringBuilder builder) {
    NFAState currentState = nfa.getStartState();

    while (true) {
      final List<Transition> transitions = currentState.getTransitions();

      if (isAcceptingState(currentState, nfa)) {
        if (transitions.isEmpty() || this.rng.nextBoolean()) {
          return;
        }
      }

      assert (!transitions.isEmpty());
      final Transition transition = transitions.get(this.rng.nextInt(transitions.size()));

      if (!transition.isEpsilonTransition()) {
        builder.append(randomCharacter(transition));
      }

      currentState = transition.getTo();
    }
  }

  private final boolean isAcceptingState(final NFAState state, final NFA nfa) {
    return nfa.getAcceptingStates().contains(state);
  }

  private final char randomCharacter(final Transition transition) {
    return randomCharacter(transition.getCharacters());
  }

  private final char randomCharacter(final CharacterSet characterSet) {
    if (characterSet instanceof CharacterSet.SingleCharacter) {
      return ((CharacterSet.SingleCharacter) characterSet).getCharacter();
    } else {
      assert (characterSet instanceof CharacterSet.CharacterGroup);

      final CharacterSet.CharacterGroup characterGroup = (CharacterSet.CharacterGroup) characterSet;
      final Group group = characterGroup.getGroup();

      if (group.isInverted()) {
        // iterate over printable (ASCII) characters and check if there is a valid one
        for (char character = ' '; character <= '~'; ++character) {
          if (characterGroup.matches(character)) {
            return character;
          }
        }

        throw new RuntimeException("did not find a valid character");
      } else {
        final List<Range> ranges = group.getRanges();
        final Range randomRange = ranges.get(this.rng.nextInt(ranges.size()));

        final char lower = getLower(randomRange);
        final char upper = getUpper(randomRange);

        return (char) (this.rng.nextInt(upper - lower + 1) + lower);
      }
    }
  }

  private final char getLower(final Range range) {
    if (range instanceof SingleCharacter) {
      return ((SingleCharacter) range).getValue();
    } else {
      assert (range instanceof CharacterRange);
      return ((CharacterRange) range).getLowerCharacter().getValue();
    }
  }

  private final char getUpper(final Range range) {
    if (range instanceof SingleCharacter) {
      return ((SingleCharacter) range).getValue();
    } else {
      assert (range instanceof CharacterRange);
      return ((CharacterRange) range).getUpperCharacter().getValue();
    }
  }

  private final boolean isValid(final String string, final LexerSymbol expectedTokenSymbol) {
    final TokenStream tokens;
    {
      try {
        tokens = this.lexer.lex(string, true);
      } catch (final Exception exception) {
        return false;
      }
    }

    if (tokens.numberOfTokens() != 1) {
      return false;
    }

    final Token lexedToken = tokens.at(0);

    return lexedToken.getTokenSymbol() == expectedTokenSymbol;
  }

}
