package gss;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.PriorityQueue;
import javax.swing.Timer;
import network.Address;
import network.Message;
import network.Network;
import network.Node;
import whiteboard.WhiteboardState;

public class GSS extends Node {

  public static final int GSS_UPDATE_PERIOD_MS = 25;

  private final PriorityQueue<GameEventMessage> inputQueue;
  private final PriorityQueue<GameEventMessage> executedQueue;
  private final PriorityQueue<GameEventMessage> outputQueue;
  private final PriorityQueue<GameEventMessage> outputtedQueue;
  private final PriorityQueue<GameState> saveStates;
  public Collection<Address> clients;
  private GameState state;
  private int gssTime;


  public GSS(Address address, Network network) {
    super(address, network);
    clients = new ArrayList<>();

    inputQueue = new PriorityQueue<>();
    executedQueue = new PriorityQueue<>(Collections.reverseOrder());
    outputQueue = new PriorityQueue<>();
    outputtedQueue = new PriorityQueue<>(Collections.reverseOrder());
    saveStates = new PriorityQueue<>(Collections.reverseOrder());
    gssTime = 0;
  }

  public void startRunning() {
    Timer t = new Timer(GSS_UPDATE_PERIOD_MS, e -> run());
    t.start();
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
      outputtedQueue.add(output);
      this.broadcast(output);
      // if multi-threading, possibly delay here so that events have time to process
    }
    outputQueue.clear();
  }

  private synchronized void broadcastStateToClients() {
    System.out.printf("[gss %s] sent state (s.t. %d, g.t. %d) to all clients\n", getAddress(),
        state.getSimTime(), this.gssTime);
//    System.out.printf("[gss %s] executed event order: ", getAddress());
//    GameEventMessage[] executedArray = executedQueue.toArray(new GameEventMessage[]{});
//    Arrays.sort(executedArray, executedQueue.comparator());
//    for (GameEventMessage executed : executedArray) {
//      System.out.printf("(%d @%s)", executed.getEvent().getSimTime(), executed.getSource());
//    }
//    System.out.println();
    sendToAllClients(new GameStateMessage(state.copy(), this.gssTime));
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

  public GameState getState() {
    return this.state;
  }

  public void setState(GameState state) {
    this.state = state;
    saveStates.add(this.state.copy());
  }
}
