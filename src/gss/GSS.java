package gss;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;
import javax.swing.Timer;
import network.Address;
import network.Message;
import network.Network;
import network.Node;

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

    Comparator<GameEvent> gameEventComparator = Comparator.comparingInt(GameEvent::getSimTime);
    Comparator<GameEventMessage> gameEventMessageComparator = (o1, o2) -> gameEventComparator.compare(o1.getEvent(), o2.getEvent());
    Comparator<GameState> gameStateComparator = Comparator.comparingInt(GameState::getSimTime);

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

    makeEventSimTimesMonotonicallyIncreasing(inputQueue);

    boolean updated = false;
    GameEventMessage input = inputQueue.poll();
    while (input != null) {
      GameEvent event = input.getEvent();
      if (event.getSimTime() < state.getSimTime()) {
        // a mis-ordering happened and we need to roll back to this time
        System.out.printf("[gss] rolling back to s.t. %d\n", event.getSimTime());
        rollbackTo(event.getSimTime());
      }

      state.applyEvent(input.getEvent());
      executedQueue.add(input);
      saveStates.add(state.copy());

      if (clients.contains(input.getSource())) {
        outputQueue.add(new GameEventMessage(input.getEvent(), this.getAddress()));
      }

      updated = true;
      input = inputQueue.poll();
    }
    return updated;
  }

  private void makeEventSimTimesMonotonicallyIncreasing(Queue<GameEventMessage> queue) {
    if (queue.isEmpty()) {
      return;
    }
    int lastSimTime = queue.peek().getEvent().getSimTime() - 1;
    for (GameEventMessage message : queue) {
      GameEvent event = message.getEvent();
      if (event.getSimTime() == lastSimTime) {
        event.setSimTime(lastSimTime + 1);
      }
      lastSimTime = event.getSimTime();
    }
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
    sendToAllClients(new GameStateMessage(state.copy()));
    System.out.printf("[gss] sent state with s.t. %d to all clients\n", state.getSimTime());
  }

  private synchronized void rollbackTo(int targetTime) {
    /*
     * 1. Roll back state to target time. Discard saved states from later times.
     * 2. Move all events in the executed queue with time > target time to the input queue.
     * 3. Cancel (send anti-messages for) any outputs with time > target time that are affected.
     */

//    System.out.println("roll back to ".concat(Float.toString(targetTime)));

    // 1. roll back state to target time
    GameState saveState = saveStates.poll();
    while (saveState != null && saveState.getSimTime() > targetTime) {
      // send an anti-message to clients for each rolled-back state
      sendAntiMessageForState(saveState);

      saveState = saveStates.poll();
    }
    if (saveState != null) {
      saveStates.add(saveState);
    }
    assert saveStates.peek() != null; // there will always be at least the initial state left
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
    // this is commented out because I made the simplifying assumption that executing game events
    // does not generate new game events. therefore, there's no need for anti-messages at this level
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

  private void sendAntiMessageForState(GameState saveState) {
    sendToAllClients(new GameStateMessage(saveState.copy(), true));
    System.out.printf("[gss] sent anti-state with s.t. %d to all clients\n", saveState.getSimTime());
  }

  private void sendToAllClients(Message message) {
    for (Address client : clients) {
      this.send(message, client);
    }
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
