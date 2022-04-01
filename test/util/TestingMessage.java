package util;

import network.Message;

public class TestingMessage extends Message {

  private final int data;

  public TestingMessage(int data) {
    this.data = data;
  }

  public int getData() {
    return data;
  }
}
