package gss;

import network.Address;
import network.Message;

public class GameStateMessage extends Message {

  private final GameState state;

  public GameStateMessage(GameState state, Address src, Address dst, int simTime, int gssTime,
      int[] vectorClock) {
    super(src, dst, simTime, gssTime, vectorClock);
    this.state = state;
  }

  public GameState getState() {
    return this.state;
  }
}
