package network;

public class Address implements Comparable<Address> {

  private final int address;

  public Address(int address) {
    this.address = address;
  }

  @Override
  public int compareTo(Address o) {
    return this.address - o.address;
  }

  @Override
  public String toString() {
    return Integer.toString(address);
  }
}
