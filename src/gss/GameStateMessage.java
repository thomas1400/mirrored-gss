package gss;

import network.Message;

public class GameStateMessage extends Message {
  private final GameState state;
  private final boolean isAntiMessage;

  public GameStateMessage(GameState state) {
    this(state, false);
  }

  public GameStateMessage(GameState state, boolean isAntiMessage) {
    this.state = state;
    this.isAntiMessage = isAntiMessage;
  }

  public GameState getState() {
    return this.state;
  }

  public boolean isAntiMessage() {
    return isAntiMessage;
  }
}
