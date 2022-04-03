package gss;

public abstract class GameEvent {
  /*
   * Interface for game events for games compatible with gss.GSS. Must be serializable.
   * Does this have other requirements? I think the gss.GameEvent is kind of just a data store.
   */

  private int simTime;

  public GameEvent(int simTime) {
    this.simTime = simTime;
  }

  public int getSimTime() {
    return simTime;
  }

  public void setSimTime(int simTime) {
    this.simTime = simTime;
  }
}
