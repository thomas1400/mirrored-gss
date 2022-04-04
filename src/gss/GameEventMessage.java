package gss;

import network.Address;
import network.Message;

public class GameEventMessage extends Message implements Comparable<GameEventMessage>{

  private final GameEvent event;
  private final Address source;
  private boolean forwarded = false;

  public GameEventMessage(GameEvent event, Address source) {
    this.event = event;
    this.source = source;
  }

  public GameEvent getEvent() {
    return this.event;
  }

  public Address getSource() {
    return this.source;
  }

  public void setForwarded(boolean forwarded) {
    this.forwarded = forwarded;
  }

  public boolean wasForwarded() {
    return this.forwarded;
  }

  @Override
  public int compareTo(GameEventMessage o) {
    if (this.getEvent().getSimTime() == o.getEvent().getSimTime()) {
      return Integer.compare(this.hashCode(), o.hashCode());
    }
    return Integer.compare(this.getEvent().getSimTime(), o.getEvent().getSimTime());
  }
}
