package util;

import java.util.ArrayList;
import java.util.List;
import network.Address;
import network.Message;
import network.Network;
import network.Node;

public class TestingNode extends Node {

  private final List<Pair<Message, Address>> received;

  public TestingNode(Address address, Network network) {
    super(address, network);
    received = new ArrayList<>();
  }

  public synchronized void handleTestingMessage(Message message, Address sender) {
    received.add(new Pair<>(message, sender));
  }

  public synchronized List<Pair<Message, Address>> getReceivedMessages() {
    return new ArrayList<>(received);
  }
}
