package i2.act.fuzzer;

import i2.act.packrat.cst.Node;

import java.util.Iterator;
import java.util.function.Consumer;

public abstract class FuzzerLoop implements Iterator<Node<?>>, Iterable<Node<?>> {

  // ===============================================================================================

  public static final int INFINITE = -1;

  public static final FuzzerLoop fixedCount(final int count, final Fuzzer fuzzer) {
    return fixedCount(count, fuzzer, null);
  }

  public static final FuzzerLoop fixedCount(final int count, final Fuzzer fuzzer,
      final Consumer<FuzzerLoop> beforeEachAttempt) {
    return new FuzzerLoop(fuzzer, beforeEachAttempt) {

      @Override
      public final boolean hasNext() {
        return count == INFINITE || this.numberOfPrograms < count;
      }

      protected final boolean keep(final Node<?> tree) {
        return true;
      }

    };
  }

  public static final FuzzerLoop infinite(final Fuzzer fuzzer) {
    return infinite(fuzzer, null);
  }

  public static final FuzzerLoop infinite(final Fuzzer fuzzer,
      final Consumer<FuzzerLoop> beforeEachAttempt) {
    return fixedCount(INFINITE, fuzzer, beforeEachAttempt);
  }

  // ===============================================================================================

  protected final Fuzzer fuzzer;
  protected final Consumer<FuzzerLoop> beforeEachAttempt;

  protected int numberOfAttempts;
  protected int numberOfPrograms;

  public FuzzerLoop(final Fuzzer fuzzer) {
    this(fuzzer, null);
  }

  public FuzzerLoop(final Fuzzer fuzzer, final Consumer<FuzzerLoop> beforeEachAttempt) {
    this.fuzzer = fuzzer;
    this.beforeEachAttempt = beforeEachAttempt;

    this.numberOfAttempts = 0;
    this.numberOfPrograms = 0;
  }

  public final int numberOfAttempts() {
    return this.numberOfAttempts;
  }

  public final int numberOfPrograms() {
    return this.numberOfPrograms;
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

    Node<?> tree = null;

    do {
      if (this.beforeEachAttempt != null) {
        this.beforeEachAttempt.accept(this);
      }

      tree = this.fuzzer.generate();

      ++this.numberOfAttempts;
    } while (!keep(tree));

    ++this.numberOfPrograms;

    return tree;
  }

  @Override
  public abstract boolean hasNext();

  protected abstract boolean keep(final Node<?> tree);

}
