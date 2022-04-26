package network;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class Message {

  /**
   * Generic class for a message to be sent on the network.
   */

  protected Address source;
  protected Address destination;
  protected int simTime;
  protected int gssTime;
  protected int[] vectorClock;
  protected Collection<Message> newlyAcknowledgedMessages;

  public Message(Address source, Address destination, int simTime, int gssTime, int[] vectorClock) {
    this.source = source;
    this.destination = destination;
    this.simTime = simTime;
    this.gssTime = gssTime;
    this.vectorClock = vectorClock;

    newlyAcknowledgedMessages = new ArrayList<>();
  }

  public void addAcknowledgedMessages(Collection<Message> messages) {
    newlyAcknowledgedMessages.addAll(messages);
  }

  public Address getSource() {
    return source;
  }

  public Address getDestination() {
    return destination;
  }

  public int getSimTime() {
    return simTime;
  }

  public int getGssTime() {
    return gssTime;
  }

  public int[] getVectorClock() {
    return vectorClock;
  }

  public Collection<Message> getNewlyAcknowledgedMessages() {
    return newlyAcknowledgedMessages;
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof Message omsg)) {
      return false;
    }

    return omsg.destination.equals(destination) && omsg.source.equals(source)
        && omsg.simTime == simTime && omsg.gssTime == gssTime && Arrays.equals(omsg.vectorClock,
        vectorClock);
  }

  @Override
  public int hashCode() {
    return Integer.hashCode(
        destination.hashCode() + source.hashCode() + Integer.hashCode(simTime) + gssTime
            + Arrays.hashCode(vectorClock));
  }
}
