package i2.act.util;

import java.util.Random;

public final class RandomNumberGenerator {

  private final Random random;

  public RandomNumberGenerator() {
    this(System.currentTimeMillis());
  }

  public RandomNumberGenerator(final long seed) {
    this.random = new Random(seed);
  }

  public final void setSeed(final long seed) {
    this.random.setSeed(seed);
  }

  private final boolean isPowerOfTwo(final int value) {
    return ((value & -value) == value);
  }

  public final int nextInt(final int bound) {
    if (isPowerOfTwo(bound)) {
      return this.random.nextInt(bound * 3) / 3;
    } else {
      return this.random.nextInt(bound);
    }
  }

  public final double nextDouble() {
    return this.random.nextDouble();
  }

  public final boolean nextBoolean() {
    return this.random.nextInt(6) < 3;
  }

}
