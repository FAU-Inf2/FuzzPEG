package i2.act.fuzzer;

import i2.act.coverage.AlternativeCoverage;

import java.util.Iterator;
import java.util.function.Consumer;

public abstract class FuzzerLoop<R> implements Iterator<R>, Iterable<R> {

  // ===============================================================================================

  public static final int INFINITE = -1;

  // ~~ fixed count ~~

  public static final <R> FuzzerLoop<R> fixedCount(final int count, final Fuzzer<R> fuzzer) {
    return fixedCount(count, fuzzer, null);
  }

  public static final <R> FuzzerLoop<R> fixedCount(final int count, final Fuzzer<R> fuzzer,
      final Consumer<FuzzerLoop<R>> beforeEachAttempt) {
    return new FixedCountFuzzerLoop<R>(count, fuzzer, beforeEachAttempt);
  }

  // ~~ infinite ~~

  public static final <R> FuzzerLoop<R> infinite(final Fuzzer<R> fuzzer) {
    return infinite(fuzzer, null);
  }

  public static final <R> FuzzerLoop<R> infinite(final Fuzzer<R> fuzzer,
      final Consumer<FuzzerLoop<R>> beforeEachAttempt) {
    return fixedCount(INFINITE, fuzzer, beforeEachAttempt);
  }

  // ~~ only additional coverage ~~

  public static final <R> FuzzerLoop<R> onlyAdditionalCoverage(final AlternativeCoverage coverage,
      final FuzzerLoop<R> baseLoop) {
    return new FilterFuzzerLoop<R>(baseLoop) {

      private int previousCoverage = 0;

      @Override
      public final void beforeEachAttempt() {
        this.previousCoverage = coverage.coveredCount();
      }

      @Override
      public final boolean cancel() {
        return coverage.isFullyCovered();
      }

      @Override
      protected final boolean keep(final R program) {
        final boolean keep = (coverage.coveredCount() > this.previousCoverage);
        return keep;
      }

    };
  }

  // ===============================================================================================

  private static final class FixedCountFuzzerLoop<R> extends FuzzerLoop<R> {

    private final int count;

    private final Fuzzer<R> fuzzer;
    private final Consumer<FuzzerLoop<R>> beforeEachAttempt;

    private int numberOfAttempts;

    public FixedCountFuzzerLoop(final int count, final Fuzzer<R> fuzzer) {
      this(count, fuzzer, null);
    }

    public FixedCountFuzzerLoop(final int count, final Fuzzer<R> fuzzer,
        final Consumer<FuzzerLoop<R>> beforeEachAttempt) {
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
    protected final R generateNext() {
      R next = null;

      ++this.numberOfAttempts;

      if (this.beforeEachAttempt != null) {
        this.beforeEachAttempt.accept(this);
      }

      next = this.fuzzer.generate();

      return next;
    }

    @Override
    public final boolean hasNext() {
      return this.count == INFINITE || this.numberOfPrograms < this.count;
    }

  }

  private abstract static class FilterFuzzerLoop<R> extends FuzzerLoop<R> {

    protected final FuzzerLoop<R> baseLoop;
    protected R next;

    public FilterFuzzerLoop(final FuzzerLoop<R> baseLoop) {
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
    protected final R generateNext() {
      if (this.next == null) {
        while (this.baseLoop.hasNext()) {
          beforeEachAttempt();
          final R next = this.baseLoop.next();

          if (keep(next)) {
            this.next = next;
            break;
          }
        }
      }

      final R next = this.next;
      this.next = null;

      return next;
    }

    protected abstract void beforeEachAttempt();

    protected abstract boolean cancel();

    protected abstract boolean keep(final R program);

  }

  // ===============================================================================================

  protected int numberOfPrograms;

  public FuzzerLoop() {
    this.numberOfPrograms = 0;
  }

  @Override
  public final FuzzerLoop<R> iterator() {
    return this;
  }

  @Override
  public final R next() {
    if (!hasNext()) {
      throw new RuntimeException("no more programs to generate");
    }

    final R next = generateNext();
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

  protected abstract R generateNext();

}
