package i2.act.fuzzer.selection;

import i2.act.coverage.AlternativeCoverage;
import i2.act.grammargraph.GrammarGraph;
import i2.act.packrat.Lexer;
import i2.act.packrat.Parser;
import i2.act.packrat.cst.visitors.TreeVisitor;
import i2.act.peg.ast.Grammar;
import i2.act.peg.builder.GrammarBuilder;
import i2.act.peg.symbols.LexerSymbol;
import i2.act.peg.symbols.ParserSymbol;
import i2.act.util.RandomNumberGenerator;

import static i2.act.peg.builder.GrammarBuilder.*;

public final class SelectionStrategyParser {

  public static final SelectionStrategy parse(final String string,
      final GrammarGraph grammarGraph, final AlternativeCoverage coverage) {
    return parse(string, grammarGraph, coverage, System.currentTimeMillis());
  }

  public static final SelectionStrategy parse(final String string,
      final GrammarGraph grammarGraph, final AlternativeCoverage coverage, final long seed) {
    return parse(string, grammarGraph, coverage, new RandomNumberGenerator(seed));
  }

  public static final SelectionStrategy parse(final String string,
      final GrammarGraph grammarGraph, final AlternativeCoverage coverage,
      final RandomNumberGenerator rng) {
    final GrammarBuilder builder = new GrammarBuilder();

    final LexerSymbol LPAREN = builder.define("LPAREN", "'('");
    final LexerSymbol RPAREN = builder.define("RPAREN", "')'");
    final LexerSymbol COMMA = builder.define("COMMA", "','");
    final LexerSymbol RAND = builder.define("RAND", "[Rr][Aa][Nn][Dd] ( [Oo][Mm] )?");
    final LexerSymbol UNIFORM = builder.define("UNIFORM", "[Uu][Nn][Ii][Ff][Oo][Rr][Mm]");
    final LexerSymbol SMALL = builder.define("SMALL", "[Ss][Mm][Aa][Ll][Ll] ( [Ee][Ss][Tt] )?");
    final LexerSymbol UNCOV = builder.define("UNCOV", "[Uu][Nn][Cc][Oo][Vv] ( [Ee][Rr][Ee][Dd] )?");
    final LexerSymbol REACHESUNCOV = builder.define(
        "REACHESUNCOV", "[Rr][Ee][Aa][Cc][Hh][Ee][Ss] [Uu][Nn][Cc][Oo][Vv] ( [Ee][Rr][Ee][Dd] )?");
    final LexerSymbol TRUE = builder.define("TRUE", "[Tt][Rr][Uu][Ee]");
    final LexerSymbol FALSE = builder.define("FALSE", "[Ff][Aa][Ll][Ss][Ee]");
    final LexerSymbol PROBABILITY = builder.define("PROBABILITY", "'1.0' | '0.' [0-9]+");
    final LexerSymbol SPACE = builder.define("SPACE", "( ' ' | '\\n' | '\\r' | '\\t' )+", true);

    final ParserSymbol root = builder.declare("root");
    final ParserSymbol selection_strategy = builder.declare("selection_strategy");
    final ParserSymbol random = builder.declare("random");
    final ParserSymbol uniform = builder.declare("uniform");
    final ParserSymbol smallest = builder.declare("smallest");
    final ParserSymbol uncovered = builder.declare("uncovered");
    final ParserSymbol reaches_uncovered = builder.declare("reaches_uncovered");

    builder.define(root,
        seq(selection_strategy, LexerSymbol.EOF));

    builder.define(selection_strategy,
        seq(alt(random, uniform, smallest, uncovered, reaches_uncovered)));

    builder.define(random,
        seq(RAND, opt(seq(LPAREN, RPAREN))));

    builder.define(uniform,
        seq(UNIFORM, opt(seq(LPAREN, RPAREN))));

    builder.define(smallest,
        seq(SMALL,
            opt(seq(LPAREN, opt(seq(opt(seq(PROBABILITY, COMMA)), selection_strategy)), RPAREN))));

    builder.define(uncovered,
        seq(UNCOV, LPAREN, selection_strategy, COMMA, selection_strategy, RPAREN));

    builder.define(reaches_uncovered,
        seq(REACHESUNCOV, LPAREN, selection_strategy, COMMA, selection_strategy,
            opt(seq(COMMA, alt(TRUE, FALSE))), RPAREN));

    final Grammar grammar = builder.build();

    final TreeVisitor<Void, SelectionStrategy> visitor =
        TreeVisitor.<Void, SelectionStrategy>leftToRight();

    // random
    visitor.add(random, (node, _void) -> {
      return new WeightedRandomSelection(rng);
    });

    // uniform
    visitor.add(uniform, (node, _void) -> {
      return new UniformRandomSelection(rng);
    });

    // smallest
    visitor.add(smallest, (node, _void) -> {
      final SelectionStrategy baseStrategy;
      final double probability;
      {
        if (node.numberOfChildren() == 1 || node.numberOfChildren() == 3) {
          baseStrategy = new WeightedRandomSelection(rng);
          probability = 1.0;
        } else if (node.numberOfChildren() == 4) {
          baseStrategy = visitor.visit(node.getChild(2));
          probability = 1.0;
        } else {
          assert (node.numberOfChildren() == 6);

          baseStrategy = visitor.visit(node.getChild(4));
          probability = Double.parseDouble(node.getChild(2).getText());
        }
      }

      return new SmallestProductionSelection(grammarGraph, baseStrategy, probability, rng);
    });

    // uncovered
    visitor.add(uncovered, (node, _void) -> {
      assert (node.numberOfChildren() == 6);

      final SelectionStrategy strategyUncovered = visitor.visit(node.getChild(2));
      final SelectionStrategy strategyCovered = visitor.visit(node.getChild(4));

      return new PreferUncoveredStrategy(coverage, strategyUncovered, strategyCovered);
    });

    // reaches_uncovered
    visitor.add(reaches_uncovered, (node, _void) -> {
      final SelectionStrategy strategyUncovered = visitor.visit(node.getChild(2));
      final SelectionStrategy strategyCovered = visitor.visit(node.getChild(4));

      final boolean strictQuantifiers;
      {
        if (node.numberOfChildren() == 8) {
          final String booleanLiteral = node.getChild(6).getText();
          strictQuantifiers = booleanLiteral.equalsIgnoreCase("true");

          if (!strictQuantifiers) {
            assert (booleanLiteral.equalsIgnoreCase("false"));
          }
        } else {
          assert (node.numberOfChildren() == 6);
          strictQuantifiers = true;
        }
      }

      return new PreferReachesUncoveredStrategy(
          grammarGraph, coverage, strategyUncovered, strategyCovered, strictQuantifiers);
    });

    // root
    visitor.add(root, (node, _void) -> {
      assert (node.numberOfChildren() == 2);
      return visitor.visit(node.getChild(0));
    });

    final Lexer lexer = Lexer.forGrammar(grammar);
    final Parser parser = Parser.fromGrammar(grammar, root, false);

    return visitor.visit(parser.parse(lexer.lex(string)), null);
  }

}
