package network;

import gss.GSSConfiguration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Node {

  private final Address address;
  private final Network network;

  // Variables for GVT calculation
  protected int[] vectorClock;
  protected int globalSimTime;
  protected int nodeIndex;
  private int lowestSimTimeUnacknowledged = -1;
  private int highestSimTimeSent;
  private final Set<Message> unacknowledgedMessages;
  private final Map<Address, Collection<Message>> newlyAcknowledgedMessages;

  public Node(Address address, Network network) {
    this.address = address;
    this.network = network;
    network.addNode(this);

    vectorClock = new int[GSSConfiguration.getNumNodes()];
    nodeIndex = GSSConfiguration.getNodeIndex(address);
    unacknowledgedMessages = new HashSet<>();
    newlyAcknowledgedMessages = new HashMap<>();
  }

  protected synchronized void send(Message message, Address dst) {
    if (message.getSimTime() > highestSimTimeSent) {
      highestSimTimeSent = message.getSimTime();
    }
    if (message.getSimTime() < lowestSimTimeUnacknowledged || lowestSimTimeUnacknowledged == -1) {
      lowestSimTimeUnacknowledged = message.getSimTime();
    }

    unacknowledgedMessages.add(message);
    if (newlyAcknowledgedMessages.get(dst) != null) {
      message.addAcknowledgedMessages(newlyAcknowledgedMessages.get(dst));
      newlyAcknowledgedMessages.get(dst).clear();
    }

    this.network.send(message, this.address, dst);
  }

  public synchronized void updateVectorClock(Message message) {
    newlyAcknowledgedMessages.putIfAbsent(message.getSource(), new ArrayList<>());
    newlyAcknowledgedMessages.get(message.getSource()).add(message);

    // Update our vector clock from the message's vector clock and metadata
    for (Message acked : message.getNewlyAcknowledgedMessages()) {
      if (unacknowledgedMessages.remove(acked)) {
        if (acked.getSimTime() == lowestSimTimeUnacknowledged) {
          setLowestSimTimeFromUnacknowledged();
        }
      }
    }

    // Do a pairwise max of vector clock entries
    vectorClock[nodeIndex] = Math.min(lowestSimTimeUnacknowledged, highestSimTimeSent);
    for (int i = 0; i < vectorClock.length; i++) {
      vectorClock[i] = Math.max(vectorClock[i], message.getVectorClock()[i]);
    }

    // Set this node's globalSimTime as the min value in the vector clock
    globalSimTime = vectorClock[0];
    for (int v : vectorClock) {
      globalSimTime = Math.min(globalSimTime, v);
    }
    System.out.printf("[node %d] updated vector clock is %s\n", nodeIndex,
        Arrays.toString(vectorClock));
  }

  private synchronized void setLowestSimTimeFromUnacknowledged() {
    int minimum = Integer.MAX_VALUE;
    for (Message message : unacknowledgedMessages) {
      minimum = Math.min(minimum, message.getSimTime());
    }
    lowestSimTimeUnacknowledged = minimum;
  }

  public Address getAddress() {
    return this.address;
  }

  public int[] getVectorClock() {
    return vectorClock.clone();
  }
}
