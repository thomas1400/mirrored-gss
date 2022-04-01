package gss;

public abstract class GameEvent {
  /*
   * Interface for game events for games compatible with gss.GSS. Must be serializable.
   * Does this have other requirements? I think the gss.GameEvent is kind of just a data store.
   */

  private final float simTime;

  public GameEvent(float simTime) {
    this.simTime = simTime;
  }

  public float getSimTime() {
    return simTime;
  }
}
