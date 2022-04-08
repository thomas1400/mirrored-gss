package gss;

import java.util.Comparator;

public abstract class GameState implements Comparable<GameState> {
  /*
   * Interface for game state for a game compatible with GSSs. Must be serializable and able to
   * apply GameEvents to update.
   */

  private int simTime;
  private int gssTime;

  public GameState(int simTime, int gssTime) {
    this.simTime = simTime;
    this.gssTime = gssTime;
  }

  public int getSimTime() {
    return simTime;
  }

  public int getGssTime() {
    return this.gssTime;
  }

  public void setGssTime(int gssTime) {
    this.gssTime = gssTime;
  }

  /**
   * Apply a gss.GameEvent to the state and return a copy of the new state
   *
   * @param event gss.GameEvent to be applied
   * @return a copy of the new state
   */
  public void applyEvent(GameEvent event) {
    this.simTime = Math.max(event.getSimTime(), this.simTime);
  }

  public abstract GameState copy();

  public int compareTo(GameState o) {
    return Comparator.comparing(GameState::getSimTime)
        .thenComparing(GameState::getGssTime)
        .thenComparing(GameState::hashCode)
        .compare(this, o);
  }
}
