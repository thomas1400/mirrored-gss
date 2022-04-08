package gss;

import java.util.HashMap;
import java.util.Map;
import network.Address;

public class GSSConfiguration {

  private static int nNodes;
  private static int nServers;
  private static int nClients;
  private static Map<Address, Integer> nodeIndices;
  private static int[] connections;
  private static Address[] serverAddresses;
  private static Address[] clientAddresses;

  public static void SetConfiguration(int nServers, int nClients, Address[] serverAddresses,
      Address[] clientAddresses, int[] connections) {

    nodeIndices = new HashMap<>();

    GSSConfiguration.nServers = nServers;
    GSSConfiguration.nClients = nClients;
    GSSConfiguration.connections = connections;
    GSSConfiguration.nNodes = nServers + nClients;
    GSSConfiguration.serverAddresses = serverAddresses;
    GSSConfiguration.clientAddresses = clientAddresses;

    for (int s = 0; s < nServers; s++) {
      nodeIndices.put(serverAddresses[s], s);
    }
    for (int c = 0; c < nClients; c++) {
      nodeIndices.put(clientAddresses[c], c+nServers);
    }
  }

  public static int getNumNodes() {
    return nNodes;
  }

  public static int getNumServers() {
    return nServers;
  }

  public static int getNumClients() {
    return nClients;
  }

  public static int getNodeIndex(Address address) {
    return nodeIndices.getOrDefault(address, -1);
  }

  public static Address[] getServerAddresses() {
    return serverAddresses.clone();
  }
}
