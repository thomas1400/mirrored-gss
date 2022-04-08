package util;

import network.Address;
import network.Message;

public class TestingMessage extends Message {

  private final int data;

  public TestingMessage(int data) {
    super(new Address(0), new Address(1), 0, 0, new int[]{0});
    this.data = data;
  }

  public int getData() {
    return data;
  }
}
