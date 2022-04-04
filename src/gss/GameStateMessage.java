package gss;

import network.Message;

public class GameStateMessage extends Message {

  private final GameState state;
  private final int gssTime;

  public GameStateMessage(GameState state, int gssTime) {
    this.state = state;
    this.gssTime = gssTime;
  }

  public GameState getState() {
    return this.state;
  }

  public int getGssTime() {
    return this.gssTime;
  }
}
