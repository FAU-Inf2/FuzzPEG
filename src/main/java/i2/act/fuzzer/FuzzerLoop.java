package i2.act.fuzzer;

import i2.act.coverage.AlternativeCoverage;
import i2.act.packrat.cst.Node;

import java.util.Iterator;
import java.util.function.Consumer;

public abstract class FuzzerLoop implements Iterator<Node<?>>, Iterable<Node<?>> {

  // ===============================================================================================

  public static final int INFINITE = -1;

  // ~~ fixed count ~~

  public static final FuzzerLoop fixedCount(final int count, final Fuzzer fuzzer) {
    return fixedCount(count, fuzzer, null);
  }

  public static final FuzzerLoop fixedCount(final int count, final Fuzzer fuzzer,
      final Consumer<FuzzerLoop> beforeEachAttempt) {
    return new FixedCountFuzzerLoop(count, fuzzer, beforeEachAttempt);
  }

  // ~~ infinite ~~

  public static final FuzzerLoop infinite(final Fuzzer fuzzer) {
    return infinite(fuzzer, null);
  }

  public static final FuzzerLoop infinite(final Fuzzer fuzzer,
      final Consumer<FuzzerLoop> beforeEachAttempt) {
    return fixedCount(INFINITE, fuzzer, beforeEachAttempt);
  }

  // ~~ only additional coverage ~~

  public static final FuzzerLoop onlyAdditionalCoverage(final AlternativeCoverage coverage,
      final FuzzerLoop baseLoop) {
    return new FilterFuzzerLoop(baseLoop) {

      private int previousCoverage = 0;

      @Override
      public final boolean cancel() {
        return (this.previousCoverage == coverage.totalCount());
      }

      @Override
      protected final boolean keep(final Node<?> tree) {
        final boolean keep = coverage.coveredCount() > this.previousCoverage;
        this.previousCoverage = coverage.coveredCount();
        return keep;
      }

    };
  }

  // ===============================================================================================

  private static final class FixedCountFuzzerLoop extends FuzzerLoop {

    private final int count;

    private final Fuzzer fuzzer;
    private final Consumer<FuzzerLoop> beforeEachAttempt;

    private int numberOfAttempts;

    public FixedCountFuzzerLoop(final int count, final Fuzzer fuzzer) {
      this(count, fuzzer, null);
    }

    public FixedCountFuzzerLoop(final int count, final Fuzzer fuzzer,
        final Consumer<FuzzerLoop> beforeEachAttempt) {
      this.count = count;
      this.fuzzer = fuzzer;
      this.beforeEachAttempt = beforeEachAttempt;

      this.numberOfAttempts = 0;
    }

    @Override
    public final int numberOfAttempts() {
      return this.numberOfAttempts;
    }

    @Override
    protected final Node<?> generateNext() {
      Node<?> tree = null;

      ++this.numberOfAttempts;

      if (this.beforeEachAttempt != null) {
        this.beforeEachAttempt.accept(this);
      }

      tree = this.fuzzer.generate();

      return tree;
    }

    @Override
    public final boolean hasNext() {
      return this.count == INFINITE || this.numberOfPrograms < this.count;
    }

  }

  private abstract static class FilterFuzzerLoop extends FuzzerLoop {

    protected final FuzzerLoop baseLoop;
    protected Node<?> next;

    public FilterFuzzerLoop(final FuzzerLoop baseLoop) {
      this.baseLoop = baseLoop;
    }

    @Override
    public final int numberOfAttempts() {
      return this.baseLoop.numberOfAttempts();
    }

    @Override
    public boolean hasNext() {
      if (this.next == null) {
        if (cancel()) {
          return false;
        }

        this.next = generateNext();
      }

      return this.next != null;
    }

    @Override
    protected final Node<?> generateNext() {
      if (this.next == null) {
        while (this.baseLoop.hasNext()) {
          final Node<?> next = this.baseLoop.next();

          if (keep(next)) {
            this.next = next;
            break;
          }
        }
      }

      final Node<?> next = this.next;
      this.next = null;

      return next;
    }

    protected abstract boolean cancel();

    protected abstract boolean keep(final Node<?> tree);

  }

  // ===============================================================================================

  protected int numberOfPrograms;

  public FuzzerLoop() {
    this.numberOfPrograms = 0;
  }

  @Override
  public final FuzzerLoop iterator() {
    return this;
  }

  @Override
  public final Node<?> next() {
    if (!hasNext()) {
      throw new RuntimeException("no more trees to generate");
    }

    final Node<?> next = generateNext();
    assert (next != null);

    ++this.numberOfPrograms;

    return next;
  }

  public final int numberOfPrograms() {
    return this.numberOfPrograms;
  }

  public abstract int numberOfAttempts();

  @Override
  public abstract boolean hasNext();

  protected abstract Node<?> generateNext();

}
