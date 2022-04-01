package gss;

import network.Message;

public class GameStateMessage extends Message {
  private final GameState state;

  public GameStateMessage(GameState state) {
    this.state = state;
  }

  public GameState state() {
    return this.state;
  }
}
