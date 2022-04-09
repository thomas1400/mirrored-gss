package gss;

import network.Address;
import network.Message;

public class GameEventMessage extends Message implements Comparable<GameEventMessage>{

  private final GameEvent event;
  private boolean forwarded = false;

  public GameEventMessage(GameEvent event, Address src, Address dst, int simTime, int gssTime, int[] vectorClock) {
    super(src, dst, simTime, gssTime, vectorClock);
    this.event = event;
  }

  public GameEvent getEvent() {
    return this.event;
  }

  public void setForwarded(boolean forwarded) {
    this.forwarded = forwarded;
  }

  public boolean wasForwarded() {
    return this.forwarded;
  }

  @Override
  public int compareTo(GameEventMessage o) {
    if (this.getSimTime() == o.getSimTime()) {
      return Integer.compare(this.hashCode(), o.hashCode());
    }
    return Integer.compare(this.getSimTime(), o.getSimTime());
  }
}
