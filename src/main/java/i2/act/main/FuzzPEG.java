package i2.act.main;

import i2.act.fuzzer.Fuzzer;
import i2.act.fuzzer.TokenJoiner;
import i2.act.grammargraph.GrammarGraph;
import i2.act.grammargraph.GrammarGraphNode;
import i2.act.grammargraph.GrammarGraphNode.AlternativeNode;
import i2.act.grammargraph.properties.MinHeightComputation;
import i2.act.peg.ast.Grammar;
import i2.act.peg.ast.visitors.NameAnalysis;
import i2.act.peg.parser.PEGParser;
import i2.act.peg.symbols.Symbol;
import i2.act.util.FileUtil;
import i2.act.util.options.ProgramArguments;
import i2.act.util.options.ProgramArgumentsParser;

import java.util.Map;

public final class FuzzPEG {

  private static final ProgramArgumentsParser argumentsParser;

  private static final String OPTION_GRAMMAR = "--grammar";
  private static final String OPTION_PRINT_GRAMMAR_GRAPH = "--printGG";
  private static final String OPTION_PRINT_MIN_HEIGHTS = "--printMinHeights";

  static {
    argumentsParser = new ProgramArgumentsParser();

    argumentsParser.addOption(OPTION_GRAMMAR, true, true, "<path to grammar>");
    argumentsParser.addOption(OPTION_PRINT_GRAMMAR_GRAPH, false);
    argumentsParser.addOption(OPTION_PRINT_MIN_HEIGHTS, false);
  }

  public static final void main(final String[] args) {
    ProgramArguments arguments = null;

    try {
      arguments = argumentsParser.parseArgs(args);
    } catch (final Exception exception) {
      abort(String.format("[!] %s", exception.getMessage()));
    }

    assert (arguments != null);

    final String grammarPath = arguments.getOption(OPTION_GRAMMAR);

    final Grammar grammar = readGrammar(grammarPath);
    final GrammarGraph grammarGraph = GrammarGraph.fromGrammar(grammar);

    if (arguments.hasOption(OPTION_PRINT_GRAMMAR_GRAPH)) {
      grammarGraph.printAsDot();
    }

    if (arguments.hasOption(OPTION_PRINT_MIN_HEIGHTS)) {
      final Map<GrammarGraphNode<?,?>, Integer> minHeights =
          MinHeightComputation.computeMinHeights(grammarGraph);

      for (final Map.Entry<GrammarGraphNode<?,?>, Integer> entry : minHeights.entrySet()) {
        final GrammarGraphNode<?,?> node = entry.getKey();
        final Integer minHeight = entry.getValue();

        if (node instanceof AlternativeNode) {
          final Symbol<?> symbol = ((AlternativeNode) node).getGrammarSymbol();

          if (symbol != null) {
            System.out.format("%30s => %d\n", symbol, minHeight);
          }
        }
      }
    }

    final TokenJoiner joiner = new TokenJoiner(grammar, " "); // TODO make separator configurable

    final Fuzzer fuzzer = new Fuzzer(grammarGraph, joiner);
    System.out.println(fuzzer.generateProgram(13));
  }

  private static final void usage() {
    System.err.format("USAGE: java %s\n", FuzzPEG.class.getSimpleName());
    System.err.println(argumentsParser.usage("  "));
  }

  private static final void abort(final String message) {
    System.err.println(message);
    usage();
    System.exit(1);
  }

  private static final Grammar readGrammar(final String grammarPath) {
    try {
      final String grammarInput = FileUtil.readFile(grammarPath);
      final Grammar grammar = PEGParser.parse(grammarInput);
      NameAnalysis.analyze(grammar);

      return grammar;
    } catch (final Exception exception) {
      abort(exception.getMessage());

      assert (false);
      return null;
    }
  }

}
