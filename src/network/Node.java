package network;

public class Node {
  private final Address address;
  private final Network network;

  public Node(Address address, Network network) {
    this.address = address;
    this.network = network;
    network.addNode(this);
  }

  protected void send(Message message, Address dst) {
    this.network.send(message, this.address, dst);
  }

  protected void broadcast(Message message) {
    this.network.broadcast(message, this.address);
  }

  /*
   * Message handlers.
   */


  /*
   * Utils
   */
  public Address getAddress() {
    return this.address;
  }

}
