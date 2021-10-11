package i2.act.fuzzer;

import i2.act.grammargraph.GrammarGraph;
import i2.act.grammargraph.GrammarGraphEdge.AlternativeEdge;
import i2.act.grammargraph.GrammarGraphEdge.SequenceEdge;
import i2.act.grammargraph.GrammarGraphNode;
import i2.act.grammargraph.GrammarGraphNode.AlternativeNode;
import i2.act.grammargraph.GrammarGraphNode.SequenceNode;
import i2.act.grammargraph.properties.MinHeightComputation;
import i2.act.packrat.Token;
import i2.act.packrat.cst.Node;
import i2.act.packrat.cst.NonTerminalNode;
import i2.act.packrat.cst.TerminalNode;
import i2.act.peg.symbols.LexerSymbol;
import i2.act.peg.symbols.ParserSymbol;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class Fuzzer {

  private final GrammarGraph grammarGraph;
  private final TokenJoiner joiner;

  private final Map<GrammarGraphNode<?,?>, Integer> minHeights;
  private final int minMaxHeight;

  private final Random rng;

  private final RandomTokenGenerator randomTokenGenerator;

  public Fuzzer(final GrammarGraph grammarGraph, final TokenJoiner joiner) {
    this(grammarGraph, joiner, System.currentTimeMillis());
  }

  public Fuzzer(final GrammarGraph grammarGraph, final TokenJoiner joiner, final long seed) {
    this.grammarGraph = grammarGraph;
    this.joiner = joiner;

    this.minHeights = MinHeightComputation.computeMinHeights(grammarGraph);

    assert (this.minHeights.containsKey(grammarGraph.getRootNode()));
    this.minMaxHeight = this.minHeights.get(grammarGraph.getRootNode());

    this.rng = new Random(seed);

    this.randomTokenGenerator = new RandomTokenGenerator(grammarGraph, this.rng);
  }

  public final void setSeed(final long seed) {
    this.rng.setSeed(seed);
  }

  public final String generateProgram(final int maxHeight) {
    final Node<?> tree = generateTree(maxHeight);
    return this.joiner.join(tree);
  }

  public final Node<?> generateTree(final int maxHeight) {
    if (maxHeight < this.minMaxHeight) {
      throw new RuntimeException(String.format(
          "maxHeight too small (the given grammar requires a maxHeight of at least %d)",
          this.minMaxHeight));
    }

    final List<Node<?>> nodes = generate(this.grammarGraph.getRootNode(), maxHeight);

    assert (nodes.size() == 1);
    return nodes.get(0);
  }

  public final List<Node<?>> generate(final AlternativeNode choice, final int maxHeight) {
    if (isTerminal(choice)) {
      return listOf(createTerminalNode(choice));
    }

    final SequenceNode chosen = chooseAlternative(viableAlternatives(choice, maxHeight));

    final List<Node<?>> children = new ArrayList<>();

    for (final SequenceEdge element : chosen.getSuccessorEdges()) {
      for (int count = count(element, maxHeight); count-- > 0;) {
        children.addAll(generate(element.getTarget(), childHeight(choice, maxHeight)));
      }
    }

    if (isProduction(choice)) {
      return listOf(createNonTerminalNode(choice, children));
    } else {
      return children;
    }
  }

  private final boolean isTerminal(final AlternativeNode choice) {
    return choice.getGrammarSymbol() instanceof LexerSymbol;
  }

  private final List<AlternativeEdge> viableAlternatives(final AlternativeNode choice,
      final int maxHeight) {
    final List<AlternativeEdge> viableAlternatives = new ArrayList<>();

    for (final AlternativeEdge alternative : choice.getSuccessorEdges()) {
      assert (this.minHeights.containsKey(alternative.getTarget()));
      if (this.minHeights.get(alternative.getTarget()) <= maxHeight) {
        viableAlternatives.add(alternative);
      }
    }

    assert (!viableAlternatives.isEmpty());
    return viableAlternatives;
  }

  private final SequenceNode chooseAlternative(final List<AlternativeEdge> alternatives) {
    assert (!alternatives.isEmpty());

    // handle fast case first
    if (alternatives.size() == 1) {
      return alternatives.get(0).getTarget();
    }

    // roulette wheel selection
    final int totalWeight = alternatives.stream()
        .map((alternative) -> alternative.getWeight())
        .reduce(0, Integer::sum);

    final int chosen = this.rng.nextInt(totalWeight) + 1;
    int weightSum = 0;

    for (final AlternativeEdge alternative : alternatives) {
      weightSum += alternative.getWeight();

      if (weightSum >= chosen) {
        return alternative.getTarget();
      }
    }

    assert (false);
    return null;
  }

  private final int count(final SequenceEdge element, final int maxHeight) {
    final SequenceEdge.Quantifier quantifier = element.getQuantifier();

    assert (this.minHeights.containsKey(element.getTarget()));
    if (this.minHeights.get(element.getTarget()) > maxHeight) {
      assert (quantifier != SequenceEdge.Quantifier.QUANT_NONE);
      return 0;
    }

    switch (quantifier) {
      case QUANT_NONE: {
        return 1;
      }
      case QUANT_OPTIONAL: {
        return (this.rng.nextInt(element.getWeight() + 1) == 0) ? 0 : 1;
      }
      default: {
        assert (quantifier == SequenceEdge.Quantifier.QUANT_STAR
            || quantifier == SequenceEdge.Quantifier.QUANT_PLUS);

        int count = (quantifier == SequenceEdge.Quantifier.QUANT_PLUS) ? 1 : 0;
        while (this.rng.nextInt(element.getWeight() + 1) != 0) {
          ++count;
        }

        return count;
      }
    }
  }

  private final int childHeight(final AlternativeNode choice, final int maxHeight) {
    return (isProduction(choice)) ? (maxHeight - 1) : (maxHeight);
  }

  private final List<Node<?>> listOf(final Node<?> node) {
    return Arrays.asList(node);
  }

  private final boolean isProduction(final AlternativeNode choice) {
    return choice.hasGrammarSymbol() && choice.getGrammarSymbol().getProduction() != null;
  }

  private final TerminalNode createTerminalNode(final AlternativeNode choice) {
    assert (choice.hasGrammarSymbol());
    assert (choice.getGrammarSymbol() instanceof LexerSymbol);

    final LexerSymbol symbol = (LexerSymbol) choice.getGrammarSymbol();
    final Token token = this.randomTokenGenerator.createRandomToken(symbol);

    return new TerminalNode(token);
  }

  private final NonTerminalNode createNonTerminalNode(final AlternativeNode choice,
      final List<Node<?>> children) {
    assert (choice.hasGrammarSymbol());
    assert (choice.getGrammarSymbol() instanceof ParserSymbol);

    final ParserSymbol symbol = (ParserSymbol) choice.getGrammarSymbol();

    return new NonTerminalNode(symbol, children);
  }

}
