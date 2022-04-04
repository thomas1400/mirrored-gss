package gss;

public abstract class GameEvent implements Comparable<GameEvent> {
  /*
   * Interface for game events for games compatible with gss.GSS. Must be serializable.
   * Does this have other requirements? I think the gss.GameEvent is kind of just a data store.
   */

  private final int simTime;

  public GameEvent(int simTime) {
    this.simTime = simTime;
  }

  public int getSimTime() {
    return simTime;
  }

  public int compareTo(GameEvent o) {
    if (this.getSimTime() == o.getSimTime()) {
      return Integer.compare(this.hashCode(), o.hashCode());
    }
    return Integer.compare(this.getSimTime(), o.getSimTime());
  }
}
