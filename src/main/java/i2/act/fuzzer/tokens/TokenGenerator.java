package i2.act.fuzzer.tokens;

import i2.act.packrat.Token;
import i2.act.peg.symbols.LexerSymbol;

public interface TokenGenerator {

  public abstract Token createToken(final LexerSymbol lexerSymbol);

}
