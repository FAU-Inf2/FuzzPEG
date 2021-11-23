package i2.act.main;

import i2.act.fuzzer.tokens.RandomTokenGenerator;
import i2.act.fuzzer.tokens.TokenGenerator;
import i2.act.fuzzer.util.TokenJoiner;
import i2.act.packrat.Token;
import i2.act.peg.ast.Grammar;
import i2.act.peg.symbols.LexerSymbol;
import i2.act.test.ExternalTestFunction;
import i2.act.test.TestFunction;
import i2.act.util.ArgumentSplitter;
import i2.act.util.FileUtil;
import i2.act.util.RandomNumberGenerator;
import i2.act.util.SafeWriter;
import i2.act.util.options.ProgramArguments;
import i2.act.util.options.ProgramArgumentsParser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RandomTokens {

  public static final String INFINITE_PROGRAMS = "inf";
  public static final int VALUE_INFINITE_PROGRAMS = -1;

  public static final int DEFAULT_BATCH_SIZE = 1000;

  private static final ProgramArgumentsParser argumentsParser;

  private static final String OPTION_GRAMMAR = "--grammar";

  private static final String OPTION_MIN_SIZE = "--min";
  private static final String OPTION_MAX_SIZE = "--max";

  private static final String OPTION_COUNT = "--count";
  private static final String OPTION_SEED = "--seed";

  private static final String OPTION_OUT = "--out";

  private static final String OPTION_BATCH_SIZE = "--batchSize";

  private static final String OPTION_JOIN = "--join";

  private static final String OPTION_FIND_BUGS = "--findBugs";

  static {
    argumentsParser = new ProgramArgumentsParser();

    argumentsParser.addOption(OPTION_GRAMMAR, true, true, "<path to grammar>");

    argumentsParser.addOption(OPTION_MIN_SIZE, true, true, "<min. size>");
    argumentsParser.addOption(OPTION_MAX_SIZE, true, true, "<max. size>");

    argumentsParser.addOption(OPTION_COUNT, false, true, "<count>");
    argumentsParser.addOption(OPTION_SEED, false, true, "<seed>");

    argumentsParser.addOption(OPTION_OUT, false, true, "<file name pattern>");

    argumentsParser.addOption(OPTION_BATCH_SIZE, false, true, "<batch size>");

    argumentsParser.addOption(OPTION_JOIN, false, true, "<separator>");

    argumentsParser.addOption(OPTION_FIND_BUGS, false, true, "<test command>");
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

    final Grammar grammar = FuzzPEG.readGrammar(grammarPath);

    final int minSize = arguments.getIntOption(OPTION_MIN_SIZE);
    final int maxSize = arguments.getIntOption(OPTION_MAX_SIZE);

    if (maxSize < minSize) {
      abort(String.format(
          "[!] value of '%s' must not be smaller than that of '%s'",
          OPTION_MAX_SIZE, OPTION_MIN_SIZE));

      assert (false);
    }

    final int count = arguments.getIntOptionOr(OPTION_COUNT, 1);

    final long initialSeed = arguments.getLongOptionOr(OPTION_SEED, System.currentTimeMillis());
    System.err.format("[i] initial seed: %d\n", initialSeed);

    final int batchSize = arguments.getIntOptionOr(OPTION_BATCH_SIZE, DEFAULT_BATCH_SIZE);

    final String fileNamePattern = arguments.getOptionOr(OPTION_OUT, null);

    final String separator = arguments.getOptionOr(OPTION_JOIN, " ");
    final TokenJoiner joiner = new TokenJoiner(grammar, separator);

    final String[] testCommandLine = getTestCommandLine(arguments);
    final boolean findBugs = (testCommandLine != null);

    if (findBugs && fileNamePattern == null) {
      abort(String.format("[!] the '%s' command line option requires the '%s' option",
          OPTION_FIND_BUGS, OPTION_OUT));

      assert (false);
      return;
    }

    final RandomNumberGenerator rng = new RandomNumberGenerator();

    final TokenGenerator tokenGenerator = new RandomTokenGenerator(grammar, rng);

    final RandomTokens randomTokens = new RandomTokens(grammar, tokenGenerator, joiner, rng);

    for (int index = 0; index < count; ++index) {
      final long seed = initialSeed + index;
      rng.setSeed(seed);

      final String program = randomTokens.generate(minSize, maxSize);

      if (fileNamePattern != null) {
        final String fileName =
            expandFileNamePattern(fileNamePattern, index, seed, batchSize);

        if (findBugs) {
          final TestFunction testFunction = new ExternalTestFunction(testCommandLine, fileName);

          if (testFunction.test(program)) {
            System.err.println("[i] program triggers a bug => keep program");
          } else {
            System.err.println("[i] program does not trigger a bug => discard program");
            FileUtil.deleteFile(fileName);
          }
        } else {
          writeProgramToFile(program, fileName);
        }
      } else {
        System.out.println(program);
      }
    }
  }

  private static final void usage() {
    System.err.format("USAGE: java %s\n", RandomTokens.class.getSimpleName());
    System.err.println(argumentsParser.usage("  "));
  }

  private static final void abort(final String message) {
    System.err.println(message);
    usage();
    System.exit(1);
  }

  private static final String expandFileNamePattern(final String fileNamePattern, final int index, 
      final long seed, final int batchSize) {
    return fileNamePattern
        .replaceAll(Pattern.quote("#{INDEX}"), Matcher.quoteReplacement("" + index))
        .replaceAll(Pattern.quote("#{SEED}"), Matcher.quoteReplacement("" + seed))
        .replaceAll(Pattern.quote("#{BATCH}"), Matcher.quoteReplacement("" + (index / batchSize)));
  }

  private static final void writeProgramToFile(final String program, final String fileName) {
    FileUtil.createPathIfNotExists(fileName);

    final SafeWriter writer = SafeWriter.openFile(fileName);
    writer.write(program);
    writer.close();
  }

  private static final String[] getTestCommandLine(final ProgramArguments arguments) {
    if (!arguments.hasOption(OPTION_FIND_BUGS)) {
      return null;
    }

    return ArgumentSplitter.splitArguments(arguments.getOption(OPTION_FIND_BUGS));
  }

  // ===============================================================================================

  private final List<LexerSymbol> lexerSymbols;
  private final TokenGenerator tokenGenerator;
  private final TokenJoiner joiner;
  private final RandomNumberGenerator rng;

  public RandomTokens(final Grammar grammar, final TokenGenerator tokenGenerator,
      final TokenJoiner joiner, final RandomNumberGenerator rng) {
    this.lexerSymbols = grammar.getLexerSymbols();
    this.tokenGenerator = tokenGenerator;
    this.joiner = joiner;
    this.rng = rng;
  }

  public final String generate(final int minSize, final int maxSize) {
    final List<Token> tokens = new ArrayList<>();

    final int size = chooseSize(minSize, maxSize);

    for (int count = 0; count < size; ++count) {
      final LexerSymbol terminal = chooseTerminal();
      final Token token = this.tokenGenerator.createToken(terminal);

      tokens.add(token);
    }

    return this.joiner.join(tokens);
  }

  private final int chooseSize(final int minSize, final int maxSize) {
    return minSize + this.rng.nextInt(maxSize - minSize + 1);
  }

  private final LexerSymbol chooseTerminal() {
    return this.lexerSymbols.get(this.rng.nextInt(this.lexerSymbols.size()));
  }

}
