package gss;

import network.Address;
import network.Network;
import network.Node;

public class GSSClient extends Node {

  protected Address gss;

  public GSSClient(Address address, Address gss, Network network) {
    super(address, network);
    this.gss = gss;
  }
}
