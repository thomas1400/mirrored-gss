package gss;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.PriorityQueue;
import javax.swing.Timer;
import network.Address;
import network.Message;
import network.Network;
import network.Node;

public class GSS extends Node {

  public static final int HEARTBEAT_PERIOD_MS = 250;
  public static final int GSS_UPDATE_PERIOD_MS = 25;
  public static final int FOSSIL_COLLECT_PERIOD_MS = 1000;

  private final PriorityQueue<GameEventMessage> inputQueue;
  private final PriorityQueue<GameEventMessage> executedQueue;
  private final PriorityQueue<GameEventMessage> outputQueue;
  private final PriorityQueue<GameState> saveStates;
  public Collection<Address> clients;
  private int gssTime;
  private GameState state;

  private Collection<Timer> timers;


  public GSS(Address address, Network network) {
    super(address, network);
    clients = new ArrayList<>();

    inputQueue = new PriorityQueue<>();
    executedQueue = new PriorityQueue<>(Collections.reverseOrder());
    outputQueue = new PriorityQueue<>();
    saveStates = new PriorityQueue<>(Collections.reverseOrder());
    gssTime = 0;
  }

  public void startRunning() {
    timers = new ArrayList<>();
    Timer t = new Timer(GSS_UPDATE_PERIOD_MS, e -> run());
    Timer g = new Timer(FOSSIL_COLLECT_PERIOD_MS, e -> collectFossils());
    Timer h = new Timer(HEARTBEAT_PERIOD_MS, e -> sendHeartbeat());
    g.setInitialDelay(FOSSIL_COLLECT_PERIOD_MS);
    timers.add(t);
    timers.add(g);
    timers.add(h);
    t.start();
    g.start();
    h.start();
  }

  private void sendHeartbeat() {
    for (Address server : GSSConfiguration.getServerAddresses()) {
      if (server.equals(this.getAddress())) {
        continue;
      }
      GameEventMessage message = new GameEventMessage(null, getAddress(), server,
              state.getSimTime(), state.getGssTime(), getVectorClock());
      this.send(message, server);
    }
  }

  public void stopRunning() {
    for (Timer t : timers) {
      t.stop();
    }
  }

  public void addClient(Node client) {
    clients.add(client.getAddress());
  }

  /**
   * Process one 'frame' of simulation, which involves incrementing simulation time and processing
   * all events in the input queue up to the new current simulation time.
   */
  public synchronized void run() {
    boolean stateUpdated = processInputQueueEvents();

    if (stateUpdated) {
      broadcastStateToClients();
    }

    if (!outputQueue.isEmpty()) {
      broadcastOutputsToGSSs();
    }
  }

  private synchronized boolean processInputQueueEvents() {
    inputQueue.removeIf((gem) -> gem.getEvent() == null);

    boolean updated = !inputQueue.isEmpty();

    GameEventMessage input = inputQueue.poll();
    while (input != null) {
      GameEvent event = input.getEvent();
//      System.out.printf("[gss %s] processing input (s.t. %d)\n", getAddress(), event.getSimTime());
      if (!executedQueue.isEmpty() && input.compareTo(executedQueue.peek()) < 0) {
        // a mis-ordering happened and we need to roll back to this time
//        System.out.printf("[gss %s] rolling back to s.t. %d\n", getAddress(), event.getSimTime());
        rollbackTo(event.getSimTime());
        inputQueue.add(input);
        input = inputQueue.poll();
        continue;
      }

      gssTime += 1;
      state.applyEvent(input.getEvent());
      state.setGssTime(gssTime);
//      System.out.printf("[gss %s] applied input (s.t. %d @ %s) new bp=%d\n", getAddress(), event.getSimTime(), input.getSource(), ((WhiteboardState) state).numberOfBlackPixels());
      executedQueue.add(input);
      saveStates.add(state.copy());

      if (!input.wasForwarded()) {
        outputQueue.add(input);
        input.setForwarded(true);
      }

      input = inputQueue.poll();
    }

    return updated;
  }

  private synchronized void broadcastOutputsToGSSs() {
    for (GameEventMessage output : outputQueue) {
      for (Address server : GSSConfiguration.getServerAddresses()) {
        if (server.equals(this.getAddress())) {
          continue;
        }
        GameEventMessage message = new GameEventMessage(output.getEvent(), getAddress(), server, output.getSimTime(), output.getGssTime(), getVectorClock());
        message.setForwarded(output.wasForwarded());
        this.send(message, server);
      }
    }

    outputQueue.clear();
  }

  private synchronized void broadcastStateToClients() {
//    System.out.printf("[gss %s] sent state (s.t. %d, g.t. %d) to all clients\n", getAddress(),
//        state.getSimTime(), this.gssTime);

    for (Address client : clients) {
      this.send(new GameStateMessage(state.copy(), this.getAddress(), client, state.getSimTime(), gssTime, getVectorClock()), client);
    }
  }

  private synchronized void rollbackTo(int targetTime) {
    /*
     * 1. Roll back state to target time. Discard saved states from later times.
     * 2. Move all events in the executed queue with time > target time to the input queue.
     * (3. Cancel (send anti-messages for) any outputs with time > target time that are affected.)
     */

    // 1. roll back state to target time
    GameState saveState = saveStates.poll();
    while (saveState != null && saveState.getSimTime() >= targetTime) {
      saveState = saveStates.poll();
    }
    saveStates.add(saveState);
    assert saveState != null;
    state = saveState.copy();
//    System.out.printf("[gss %s] after rollback bp = %d, st = %d\n", getAddress(), ((WhiteboardState) state).numberOfBlackPixels(), state.getSimTime());

    // 2. move rolled-back events back to the input queue
    GameEventMessage executed = executedQueue.poll();
    while (executed != null && executed.getEvent().getSimTime() >= targetTime) {
      inputQueue.add(executed);
      executed = executedQueue.poll();
    }
    if (executed != null) {
      executedQueue.add(executed); // add the last one back
    }
  }

  private synchronized void collectFossils() {

//    System.out.printf("[gss %s] about to run fossil collect, gst %d, vc %s\n", getAddress(), globalSimTime,
//        Arrays.toString(vectorClock));
//    System.out.printf("[gss %s] initial queue lengths: %d, %d, %d, %d\n", getAddress(), saveStates.size(), inputQueue.size(), executedQueue.size(), outputQueue.size());

    PriorityQueue<GameState> saveStatesReversed = new PriorityQueue<>(saveStates.comparator().reversed());
    saveStatesReversed.addAll(saveStates);
    saveStates.clear();

    GameState saveState = saveStatesReversed.poll();
    GameState last = saveState;
    while (saveState != null && saveState.getSimTime() < globalSimTime) {
      last = saveState;
      saveState = saveStatesReversed.poll();
    }
    saveStatesReversed.add(last);
    saveStates.addAll(saveStatesReversed);

//    inputQueue.removeIf((i) -> i.getSimTime() < globalSimTime);
    executedQueue.removeIf((e) -> e.getSimTime() < globalSimTime);
    outputQueue.removeIf((o) -> o.getSimTime() < globalSimTime);

//    System.out.printf("[gss %s] ran fossil collect, gst %d, vc %s\n", getAddress(), globalSimTime,
//        Arrays.toString(vectorClock));
//    System.out.printf("[gss %s] queue lengths: %d, %d, %d, %d\n", getAddress(), saveStates.size(), inputQueue.size(), executedQueue.size(), outputQueue.size());

//    System.gc();
  }

  /* --------------
   * Message Handlers
   * -------------- */
  public synchronized void handleGameEventMessage(Message m, Address sender) {
    if (!(m instanceof GameEventMessage gem)) {
      throw new RuntimeException("Attempted to handle wrong type of message");
    }

    inputQueue.add(gem);
  }

  public GameState getState() {
    return this.state;
  }

  public void setState(GameState state) {
    this.state = state;
    saveStates.add(this.state.copy());
  }
}
