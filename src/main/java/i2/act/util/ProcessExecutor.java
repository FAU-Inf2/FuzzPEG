package i2.act.util;

public final class ProcessExecutor {

  public static final int EXIT_VALUE_SUCCESS = 0;

  public static final int execute(final String... command) {
    final ProcessBuilder processBuilder = new ProcessBuilder(command);
    processBuilder.inheritIO();

    try {
      final Process process = processBuilder.start();
      process.waitFor();
      return process.exitValue();
    } catch (final Throwable throwable) {
      throw new RuntimeException("unable to execute command", throwable);
    }
  }

  public static final boolean executeAndCheck(final String... command) {
    return execute(command) == EXIT_VALUE_SUCCESS;
  }

}
