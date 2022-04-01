package gss;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.PriorityQueue;
import javax.swing.Timer;
import network.Address;
import network.Message;
import network.Network;
import network.Node;
import whiteboard.WhiteboardEvent;

public class GSS extends Node {

  public static final int GSS_UPDATE_PERIOD_MS = 25;

  private GameState state;
  public Collection<Address> clients;

  // Components of Time Warp
  private final PriorityQueue<GameEventMessage> inputQueue;
  private final PriorityQueue<GameEventMessage> executedQueue;
  private final PriorityQueue<GameEventMessage> outputQueue;
  private final PriorityQueue<GameEventMessage> outputtedQueue;
  private final PriorityQueue<GameState> saveStates;


  public GSS(Address address, Network network) {
    super(address, network);
    clients = new ArrayList<>();

    Comparator<GameEvent> gameEventComparator = (o1, o2) -> Float.compare(o1.getSimTime(), o2.getSimTime());
    Comparator<GameEventMessage> gameEventMessageComparator = (o1, o2) -> gameEventComparator.compare(o1.getEvent(), o2.getEvent());
    Comparator<GameState> gameStateComparator = (o1, o2) -> Float.compare(o1.getSimTime(), o2.getSimTime());

    inputQueue = new PriorityQueue<>(gameEventMessageComparator);
    executedQueue = new PriorityQueue<>(gameEventMessageComparator.reversed());
    outputQueue = new PriorityQueue<>(gameEventMessageComparator);
    outputtedQueue = new PriorityQueue<>(gameEventMessageComparator.reversed());
    saveStates = new PriorityQueue<>(gameStateComparator.reversed());
  }

  public void startRunning() {
    Timer t = new Timer(GSS_UPDATE_PERIOD_MS, e -> run());
    t.start();
  }

  public void setState(GameState state) {
    this.state = state;
    saveStates.add(this.state.copy());
  }

  public void addClient(Node client) {
    clients.add(client.getAddress());
  }

  /**
   * Process one 'frame' of simulation, which involves incrementing simulation time and
   * processing all events in the input queue up to the new current simulation time.
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
    boolean updated = false;
    GameEventMessage input = inputQueue.poll();
    while (input != null) {
      GameEvent event = input.getEvent();
      if (event.getSimTime() < state.getSimTime()) {
        // a mis-ordering happened and we need to roll back to this time
        rollbackTo(event.getSimTime());
      }

      state.applyEvent(input.getEvent());
      saveStates.add(state.copy());

      if (clients.contains(input.getSource())) {
        outputQueue.add(new GameEventMessage(input.getEvent(), this.getAddress()));
      }
      updated = true;
      executedQueue.add(input);

      input = inputQueue.poll();
    }
    return updated;
  }

  private synchronized void broadcastOutputsToGSSs() {
    for (GameEventMessage output : outputQueue) {
      outputtedQueue.add(output);
      this.broadcast(output);
      // if multi-threading, possibly delay here so that events have time to process
    }
    outputQueue.clear();
  }

  private synchronized void broadcastStateToClients() {
    for (Address client : clients) {
      this.send(new GameStateMessage(state.copy()), client);
      System.out.printf("[gss] sent state with time %f to client with address %s\n", state.getSimTime(), client);
    }
  }

  private synchronized void rollbackTo(float targetTime) {
    /*
     * 1. Roll back state to target time. Discard saved states from later times.
     * 2. Move all events in the executed queue with time > target time to the input queue.
     * 3. Cancel (send anti-messages for) any outputs with time > target time that are affected.
     */

//    System.out.println("roll back to ".concat(Float.toString(targetTime)));

    // 1. roll back state to target time
    saveStates.removeIf((gs) -> gs.getSimTime() > targetTime);
    assert saveStates.peek() != null;
    state = saveStates.peek().copy();

    // 2. move rolled-back events back to the input queue
    GameEventMessage executed = executedQueue.poll();
    while (executed != null && executed.getEvent().getSimTime() > targetTime) {
      inputQueue.add(executed);
      executed = executedQueue.poll();
    }
    if (executed != null) {
      executedQueue.add(executed); // add the last one back
    }

    /*
    // 3. cancel outputs
    GameEventMessage output = outputtedQueue.poll();
    while (output != null && output.getEvent().getSimTime() > targetTime) {
      outputQueue.add(output.toAntiMessage());
      output = outputtedQueue.poll();
    }
    if (output != null) {
      outputtedQueue.add(output);
    }
     */
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


  /*
   * Methods exposed for testing
   */
  public GameState getState() {
    return this.state;
  }
}
