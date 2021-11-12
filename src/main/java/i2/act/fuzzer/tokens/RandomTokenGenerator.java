package i2.act.fuzzer.tokens;

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
          tokenValue = createRandomString(nfa);
        } while (this.checkTokens && !isValid(tokenValue, lexerSymbol));
      }
    }

    return new Token(lexerSymbol, tokenValue);
  }

  private final String createRandomString(final NFA nfa) {
    final StringBuilder builder = new StringBuilder();

    NFAState currentState = nfa.getStartState();

    while (true) {
      if (isAcceptingState(currentState, nfa) && stopHere(currentState)) {
        return builder.toString();
      }

      final Transition transition = chooseTransition(currentState);

      if (!transition.isEpsilonTransition()) {
        builder.append(chooseCharacter(transition));
      }

      currentState = transition.getTo();
    }
  }

  private final boolean isAcceptingState(final NFAState state, final NFA nfa) {
    return nfa.getAcceptingStates().contains(state);
  }

  private final boolean stopHere(final NFAState state) {
    return state.getTransitions().isEmpty() || this.rng.nextBoolean();
  }

  private final Transition chooseTransition(final NFAState state) {
    final List<Transition> transitions = state.getTransitions();

    assert (!transitions.isEmpty());
    return transitions.get(this.rng.nextInt(transitions.size()));
  }

  private final char chooseCharacter(final Transition transition) {
    return chooseCharacter(transition.getCharacters());
  }

  private final char chooseCharacter(final CharacterSet characterSet) {
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
