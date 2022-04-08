package gss;

import static org.awaitility.Awaitility.await;

import java.awt.Point;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import network.Address;
import network.Network;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import util.TestingNetwork;
import whiteboard.WhiteboardClient;
import whiteboard.WhiteboardEvent;
import whiteboard.WhiteboardState;

public class TestGSS {

  private TestingNetwork network;
  private Map<Integer, GSS> servers;
  private Map<Integer, WhiteboardClient> clients;
  private int nServers;
  private int nClients;
  private Random random;

  @BeforeEach
  public void setup() {
    random = new Random();
  }

  public void setupNetwork(float txSuccessRate, int nServers, int nClients, int[] connections) {
    // Set up the TestingNetwork and GSSs
    network = new TestingNetwork(txSuccessRate);
    servers = new HashMap<>();
    clients = new HashMap<>();
    this.nServers = nServers;
    this.nClients = nClients;

    // Setup GSSConfiguration. Do this first b/c nodes use it in initialization
    Address[] serverAddresses = new Address[nServers];
    Address[] clientAddresses = new Address[nClients];
    for (int s = 0; s < nServers; s++) {
      serverAddresses[s] = new Address(s);
    }
    for (int c = 0; c < nClients; c++) {
      clientAddresses[c] = new Address(nServers + c);
    }
    GSSConfiguration.SetConfiguration(nServers, nClients, serverAddresses, clientAddresses, connections);

    for (int s = 0; s < nServers; s++) {
      GSS server = new GSS(serverAddresses[s], network);
      servers.put(s, server);
    }
    for (int c = 0; c < nClients; c++) {
      WhiteboardClient client = new WhiteboardClient(clientAddresses[c],
          gss(connections[c]).getAddress(), network);
      gss(connections[c]).addClient(client);
      gss(connections[c]).setState(client.getState().copy());
      clients.put(c, client);
    }

    for (int s = 0; s < nServers; s++) {
      gss(s).startRunning();
    }
  }

  @AfterEach
  public void tearDownNetwork() {
    for (int s = 0; s < nServers; s++) {
      gss(s).stopRunning();
    }
  }

  @Test
  public synchronized void testOneServerOneRequestNoConcurrency() {
    final int T = 20;

    setupNetwork(Network.RELIABLE_TX, 1, 2, new int[]{0, 0});

    // Then send a few events from each client and wait for the other client to receive it
    for (int i = 0; i < T; i++) {
      WhiteboardEvent delta = randomWhiteboardEvent(client(0));
      client(0).acceptTestingEvent(delta);
      await().atMost(Duration.ofSeconds(1))
          .until(() -> client(0).getState().equals(gss(0).getState()));
    }

    await().atMost(Duration.ofSeconds(10))
        .until(() -> client(0).getState().equals(client(1).getState()));
  }

  @Test
  public synchronized void testOneServerNoConcurrency() {
    final int T = 100;

    setupNetwork(Network.RELIABLE_TX, 1, 2, new int[]{0, 0});

    sendRandomEvents(T, 0);
    awaitStateConvergence(5);
    network.pause();
    sendRandomEvents(T, 0);
    network.unpause();
    awaitStateConvergence(5);
  }

  @Test
  public synchronized void testOneServerNoConcurrencyUnreliable() {
    final int T = 100;

    setupNetwork(0.8f, 1, 2, new int[]{0, 0});

    sendRandomEvents(T, 0);
    awaitStateConvergence(5);
    network.pause();
    sendRandomEvents(T, 0);
    network.unpause();
    awaitStateConvergence(5);
  }

  @Test
  public synchronized void testOneServer() {
    final int T = 5;

    setupNetwork(Network.RELIABLE_TX, 1, 4, new int[]{0, 0, 0, 0});

    randomEventsOneServer(T);
  }

  @Test
  public synchronized void testOneServerUnreliable() {
    final int T = 5;

    setupNetwork(Network.UNRELIABLE_TX, 1, 4, new int[]{0, 0, 0, 0});

    randomEventsOneServer(T);
  }

  @Test
  public synchronized void testTwoServers() {
    final int T = 5;

    setupNetwork(Network.RELIABLE_TX, 2, 4, new int[]{0, 0, 1, 1});

    randomEventsTwoServers(T);
  }

  @Test
  public synchronized void testTwoServersUnreliable() {
    final int T = 5;

    setupNetwork(Network.UNRELIABLE_TX, 2, 4, new int[]{0, 0, 1, 1});

    randomEventsTwoServers(T);
  }

  private void randomEventsOneServer(int T) {
    sendRandomEvents(T, 0);
    awaitStateConvergence(5);
    network.pause();
    sendRandomEvents(T, 0);
    sendRandomEvents(T, 1);
    network.unpause();
    awaitStateConvergence(5);
    sendRandomEvents(T, 0);
    sendRandomEvents(T, 1);
    sendRandomEvents(T, 2);
    sendRandomEvents(T, 1);
    sendRandomEvents(T, 0);
    network.unpause();
    awaitStateConvergence(5);
    sendRandomEvents(T, 0);
    sendRandomEvents(T, 1);
    sendRandomEvents(T, 2);
    sendRandomEvents(T, 1);
    sendRandomEvents(T, 2);
    sendRandomEvents(T, 1);
    sendRandomEvents(T, 0);
    awaitStateConvergence(5);
  }

  private void randomEventsTwoServers(int T) {
    sendRandomEvents(T, 0);
    awaitStateConvergence(5);
    network.pause();
    sendRandomEvents(T, 0);
    sendRandomEvents(T, 1);
    network.unpause();
    awaitStateConvergence(5);
    sendRandomEvents(T, 0);
    sendRandomEvents(T, 1);
    sendRandomEvents(T, 2);
    sendRandomEvents(T, 1);
    sendRandomEvents(T, 0);
    sendRandomEvents(T, 2);
    sendRandomEvents(T, 3);
    sendRandomEvents(T, 1);
    sendRandomEvents(T, 0);
    awaitStateConvergence(5);
    network.pause();
    sendRandomEvents(T, 0);
    sendRandomEvents(T, 3);
    sendRandomEvents(T, 2);
    sendRandomEvents(T, 1);
    sendRandomEvents(T, 2);
    sendRandomEvents(T, 3);
    sendRandomEvents(T, 1);
    sendRandomEvents(T, 0);
    network.unpause();
    sendRandomEvents(T, 2);
    sendRandomEvents(T, 3);
    sendRandomEvents(T, 1);
    sendRandomEvents(T, 0);
    awaitStateConvergence(5);
  }

  private void sendRandomEvents(int number, int clientNum) {
    // Send events one at a time
    for (int i = 0; i < number; i++) {
      WhiteboardEvent delta = randomWhiteboardEvent(client(clientNum));
      client(clientNum).acceptTestingEvent(delta);
    }
  }

  private void awaitStateConvergence(int timeoutSec) {
    await().atMost(Duration.ofSeconds(timeoutSec)).until(() -> {
      GameState reference = gss(nServers - 1).getState();
      boolean converged = true;
      for (int c = 0; c < nClients; c++) {
        if (!client(c).getState().equals(reference)) {
          System.out.printf("Client %d state (st %d gt %d) does not equal reference (st %d gt %d)\n", c,
              client(c).getState().getSimTime(), client(c).getState().getGssTime(), reference.getSimTime(), reference.getGssTime());
          converged = false;
        }
      }
      for (int s = 0; s < nServers; s++) {
        if (!gss(s).getState().equals(reference)) {
          System.out.printf("Server %d state (st %d gt %d) does not equal reference (st %d gt %d)\n", s,
              gss(s).getState().getSimTime(), gss(s).getState().getGssTime(), reference.getSimTime(), reference.getGssTime());
          converged = false;
        }
      }
      return converged;
    });
  }

  private WhiteboardEvent randomWhiteboardEvent(WhiteboardClient client) {
    WhiteboardState state = client.getState();
    int simTime = state.getSimTime() + 1;
    int width = state.getBoard().getWidth();
    int height = state.getBoard().getHeight();
    Point start = randomPoint(width, height);
    Point end = randomPoint(width, height);
    return new WhiteboardEvent(start, end, simTime);
  }

  private Point randomPoint(int width, int height) {
    return new Point(random.nextInt(0, width), random.nextInt(0, height));
  }

  private GSS gss(int s) {
    return servers.get(s);
  }

  private WhiteboardClient client(int c) {
    return clients.get(c);
  }
}
