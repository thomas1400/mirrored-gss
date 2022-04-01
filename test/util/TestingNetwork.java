package util;

import java.util.LinkedList;
import java.util.Queue;
import network.Address;
import network.Message;
import network.Network;

public class TestingNetwork extends Network {

  private boolean paused;
  private final Queue<Triple<Message, Address, Address>> queuedMessages;

  public TestingNetwork(float txSuccessRate) {
    super(txSuccessRate);
    paused = false;
    queuedMessages = new LinkedList<>();
  }

  public void setSuccessRate(float txSuccessRate) {
    this.txSuccessRate = txSuccessRate;
  }

  public void pause() {
    paused = true;
  }

  public void unpause() {
    paused = false;

    // send all pending messages
    for (Triple<Message, Address, Address> t : queuedMessages) {
      send(t.getFirst(), t.getSecond(), t.getThird());
    }
  }

  @Override
  public void send(Message message, Address src, Address dst) {
    if (paused) {
      queuedMessages.add(new Triple<>(message, src, dst));
    } else {
      super.send(message, src, dst);
    }
  }

  public void deliverWhilePaused(Message message, Address src, Address dst) {
    super.send(message, src, dst);
  }

}
