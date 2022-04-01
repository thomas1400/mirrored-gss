package gss;

import network.Address;
import network.Message;

public class GameEventMessage extends Message {
  private final GameEvent event;
  private final Address source;
  private final boolean isAntiMessage;

  public GameEventMessage(GameEvent event, Address source) {
    this(event, source, false);
  }

  public GameEventMessage(GameEvent event, Address source, boolean isAntiMessage) {
    this.event = event;
    this.source = source;
    this.isAntiMessage = isAntiMessage;
  }

  public GameEvent getEvent() {
    return this.event;
  }

  public Address getSource() {
    return this.source;
  }

  public boolean isAntiMessage() {
    return this.isAntiMessage;
  }

  public GameEventMessage toAntiMessage() {
    return new GameEventMessage(event, source, !isAntiMessage);
  }
}
