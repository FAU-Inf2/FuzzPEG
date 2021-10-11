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
import i2.act.util.SafeWriter;
import i2.act.util.options.ProgramArguments;
import i2.act.util.options.ProgramArgumentsParser;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FuzzPEG {

  public static final String INFINITE_PROGRAMS = "inf";
  public static final int VALUE_INFINITE_PROGRAMS = -1;

  public static final int DEFAULT_BATCH_SIZE = 1000;

  private static final ProgramArgumentsParser argumentsParser;

  private static final String OPTION_GRAMMAR = "--grammar";
  private static final String OPTION_MAX_HEIGHT = "--maxHeight";

  private static final String OPTION_PRINT_GRAMMAR_GRAPH = "--printGG";
  private static final String OPTION_PRINT_MIN_HEIGHTS = "--printMinHeights";

  private static final String OPTION_SEED = "--seed";
  private static final String OPTION_COUNT = "--count";
  private static final String OPTION_BATCH_SIZE = "--batchSize";

  private static final String OPTION_JOIN = "--join";

  private static final String OPTION_OUT = "--out";

  static {
    argumentsParser = new ProgramArgumentsParser();

    argumentsParser.addOption(OPTION_GRAMMAR, true, true, "<path to grammar>");
    argumentsParser.addOption(OPTION_MAX_HEIGHT, true, true, "<max. height>");

    argumentsParser.addOption(OPTION_PRINT_GRAMMAR_GRAPH, false);
    argumentsParser.addOption(OPTION_PRINT_MIN_HEIGHTS, false);

    argumentsParser.addOption(OPTION_SEED, false, true, "<seed>");
    argumentsParser.addOption(OPTION_COUNT, false, true, "<count>");
    argumentsParser.addOption(OPTION_BATCH_SIZE, false, true, "<batch size>");

    argumentsParser.addOption(OPTION_JOIN, false, true, "<separator>");

    argumentsParser.addOption(OPTION_OUT, false, true, "<file name pattern>");
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

    final long initialSeed = arguments.getLongOptionOr(OPTION_SEED, System.currentTimeMillis());
    final int count;
    {
      if (arguments.hasOption(OPTION_COUNT)) {
        final String countValue = arguments.getOption(OPTION_COUNT);

        if (INFINITE_PROGRAMS.equalsIgnoreCase(countValue)) {
          count = VALUE_INFINITE_PROGRAMS;
        } else {
          count = arguments.getIntOption(OPTION_COUNT);
        }
      } else {
        count = 1;
      }
    }
    final int batchSize = arguments.getIntOptionOr(OPTION_BATCH_SIZE, DEFAULT_BATCH_SIZE);

    final int maxHeight = arguments.getIntOption(OPTION_MAX_HEIGHT);

    final String separator = arguments.getOptionOr(OPTION_JOIN, " ");
    final TokenJoiner joiner = new TokenJoiner(grammar, separator);

    final String fileNamePattern = arguments.getOptionOr(OPTION_OUT, null);

    final Fuzzer fuzzer = new Fuzzer(grammarGraph, joiner);

    for (int index = 0; index < count || count == VALUE_INFINITE_PROGRAMS; ++index) {
      final long seed = initialSeed + index;
      fuzzer.setSeed(seed);

      final String program = fuzzer.generateProgram(maxHeight);

      if (fileNamePattern == null) {
        System.out.println(program);
      } else {
        final String fileName =
            expandFileNamePattern(fileNamePattern, maxHeight, index, seed, batchSize);

        FileUtil.createPathIfNotExists(fileName);

        final SafeWriter writer = SafeWriter.openFile(fileName);
        writer.write(program);
        writer.close();
      }
    }
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

  private static final String expandFileNamePattern(final String fileNamePattern,
      final int maxHeight, final int index, final long seed, final int batchSize) {
    return fileNamePattern
        .replaceAll(Pattern.quote("#{MAX_HEIGHT}"), Matcher.quoteReplacement("" + maxHeight))
        .replaceAll(Pattern.quote("#{INDEX}"), Matcher.quoteReplacement("" + index))
        .replaceAll(Pattern.quote("#{SEED}"), Matcher.quoteReplacement("" + seed))
        .replaceAll(Pattern.quote("#{BATCH}"), Matcher.quoteReplacement("" + (index / batchSize)));
  }

}
