package i2.act.coverage;

import i2.act.grammargraph.GrammarGraphEdge.Alternative;

public interface Coverage {

  // TODO make more generic?
  public abstract void covered(final Alternative alternative);

  public abstract int totalCount();

  public abstract int coveredCount();

  public abstract int missingCount();

  public abstract boolean isFullyCovered();

}
