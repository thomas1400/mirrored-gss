import gss.GSS;
import gss.GSSConfiguration;
import java.awt.Point;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.Timer;
import network.Address;
import network.Network;
import whiteboard.WhiteboardClient;
import whiteboard.WhiteboardEvent;
import whiteboard.WhiteboardState;

public class Main {

  private static Map<Integer, GSS> servers;
  private static Map<Integer, WhiteboardClient> clients;
  private static Random random;

  public static void main(String[] args) {
    final int nClients = 6;

    random = new Random();

    setupNetwork(0.8f, 2, nClients, new int[]{0, 0, 0, 1, 1, 1});

    for (int c = 0; c < nClients; c++) {
      setupTurtleForClient(c);
    }
  }

  public static void setupNetwork(float txSuccessRate, int nServers, int nClients,
      int[] connections) {
    // Set up the TestingNetwork and GSSs
    Network network = new Network(txSuccessRate);
    servers = new HashMap<>();
    clients = new HashMap<>();

    // Setup GSSConfiguration. Do this first b/c nodes use it in initialization
    Address[] serverAddresses = new Address[nServers];
    Address[] clientAddresses = new Address[nClients];
    for (int s = 0; s < nServers; s++) {
      serverAddresses[s] = new Address(s);
    }
    for (int c = 0; c < nClients; c++) {
      clientAddresses[c] = new Address(nServers + c);
    }
    GSSConfiguration.SetConfiguration(nServers, nClients, serverAddresses, clientAddresses,
        connections);

    for (int s = 0; s < nServers; s++) {
      GSS server = new GSS(serverAddresses[s], network);
      servers.put(s, server);
    }
    for (int c = 0; c < nClients; c++) {
      WhiteboardClient client = new WhiteboardClient(clientAddresses[c],
          gss(connections[c]).getAddress(), network);
      client.moveWindow(c, 2, 3);
      gss(connections[c]).addClient(client);
      gss(connections[c]).setState(client.getState().copy());
      clients.put(c, client);
    }

    for (int s = 0; s < nServers; s++) {
      gss(s).startRunning();
    }
    for (int c = 0; c < nClients; c++) {
      client(c).startRunning();
    }
  }

  private static void setupTurtleForClient(int c) {
    // Every 5 seconds, pick a random point on the canvas. Then, move towards it,
    // sending a new WhiteboardEvent every 10ms.

    AtomicReference<Point> current = new AtomicReference<>(randomPointOnCanvas(client(c)));
    AtomicReference<Point> startOfMovement = new AtomicReference<>(current.get().getLocation());
    AtomicReference<Timer> turtleMove = new AtomicReference<>(new Timer(1000, (e) -> {
    }));

    final int TARGET_PERIOD = 10000;
    final int MOVE_PERIOD = 1000 / 24;

    Timer turtleTarget = new Timer(TARGET_PERIOD, (te) -> {
      Point target = randomPointOnCanvas(client(c));

      turtleMove.get().stop();
      startOfMovement.set(current.get());

      AtomicInteger frames = new AtomicInteger();

      turtleMove.set(new Timer(MOVE_PERIOD, (me) -> {
        Point next = lerp(startOfMovement.get(), target,
            frames.get() * ((float) MOVE_PERIOD) / TARGET_PERIOD);
        client(c).acceptGameEvent(
            new WhiteboardEvent(current.get(), next, client(c).getState().getSimTime() + 1));
        current.set(next);
        frames.getAndIncrement();

        client(c).drawTurtleAt(current.get());
      }));

      turtleMove.get().start();
    });

    turtleTarget.setInitialDelay(1000);
    turtleTarget.start();
  }


  private static Point randomPointOnCanvas(WhiteboardClient client) {
    WhiteboardState state = client.getState();
    int width = state.getBoard().getWidth();
    int height = state.getBoard().getHeight();
    return randomPoint(width, height);
  }

  private static Point randomPoint(int width, int height) {
    return new Point(random.nextInt(0, width), random.nextInt(0, height));
  }

  private static Point lerp(Point s, Point t, float pct) {
    int x = (int) (s.getX() + (t.getX() - s.getX()) * pct);
    int y = (int) (s.getY() + (t.getY() - s.getY()) * pct);
    return new Point(x, y);
  }

  private static GSS gss(int s) {
    return servers.get(s);
  }

  private static WhiteboardClient client(int c) {
    return clients.get(c);
  }
}
