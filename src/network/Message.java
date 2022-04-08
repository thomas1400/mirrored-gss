package network;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Message {
  // All messages need to send a list of new ACKs for received messages of the same type
  // ACKs contain the sim time and original source address of the message
  // As long as we do this per type of message it should be enough to uniquely identify

  // All nodes maintain a list of messages that were sent but not yet acked
  // For each ACK that's received the corresponding message is taken off that list
  // If the sim time of that message is equal to the LBVT, increase LBVT to min(simTime, simTime
  //  of messages yet to be ACKed)

  // Send this vector clock with each message. Upon receiving any message, nodes update their
  // vector clock with pairwise maximums. GVT is no lower than the minimum element of the vector
  // clock at any given node and queued items with sim time lower than that can be deleted.

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
}
