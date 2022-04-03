package gss;

import network.Address;
import network.Message;
import network.Network;
import network.Node;
import whiteboard.WhiteboardState;

public class GSSClient extends Node {

  protected Address gss;

  public GSSClient(Address address, Address gss, Network network) {
    super(address, network);
    this.gss = gss;
  }
}
