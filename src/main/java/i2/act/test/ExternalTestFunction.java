package i2.act.test;

import i2.act.util.ArgumentSplitter;
import i2.act.util.FileUtil;
import i2.act.util.ProcessExecutor;

public final class ExternalTestFunction implements TestFunction {

  private final String[] commandLine;
  private final String fileName;

  public ExternalTestFunction(final String[] commandLine, final String fileName) {
    this.commandLine = commandLine;
    this.fileName = fileName;
  }

  @Override
  public final boolean test(final String program) {
    FileUtil.createPathIfNotExists(this.fileName);
    FileUtil.writeToFile(program, this.fileName);

    // execute external command
    final String[] commandLine = ArgumentSplitter.appendArgument(this.commandLine, this.fileName);

    final boolean containsBug = !ProcessExecutor.executeAndCheck(commandLine);

    return containsBug;
  }

}
