package gss;

import java.util.ArrayList;
import java.util.List;

public abstract class GameState {
  /*
   * Interface for game state for a game compatible with GSSs. Must be serializable and able to
   * apply GameEvents to update.
   */

  private int simTime;

  public GameState(int simTime) {
    this.simTime = simTime;
  }

  public int getSimTime() {
    return simTime;
  }

  /**
   * Apply a gss.GameEvent to the state and return a copy of the new state
   * @param event gss.GameEvent to be applied
   * @return a copy of the new state
   */
  public void applyEvent(GameEvent event) {
    this.simTime = Math.max(event.getSimTime(), this.simTime);
  }

  public abstract GameState copy();
}
