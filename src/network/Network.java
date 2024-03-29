package network;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class Network {

  public static final float RELIABLE_TX = 1.0f;
  public static final float UNRELIABLE_TX = 0.8f;
  public static final long RETRY_MILLIS = 50;

  private final Map<Address, Node> nodes;
  private final Random random;
  protected float txSuccessRate;

  public Network(float txSuccessRate) {
    this.txSuccessRate = txSuccessRate;
    nodes = new HashMap<>();
    random = new Random();
  }

  public void addNode(Node node) {
    nodes.put(node.getAddress(), node);
  }

  public synchronized void send(Message message, Address src, Address dst) {
    if (!nodes.containsKey(dst)) {
      throw new RuntimeException("Nonexistent destination address for message");
    }

    Timer retryTimer = new Timer();

    // Model network unreliability using tx success rate and retry interval
    TimerTask tryTransmit = new TimerTask() {
      @Override
      public void run() {
        if (random.nextFloat(0f, 1f) <= txSuccessRate) {
          // Using the reflection logic from dslabs as inspiration
          Node dstNode = nodes.get(dst);
          Method handler = getMessageHandler(message, dstNode);
          if (handler == null) {
            throw new RuntimeException(
                "Attempted to send message to node without appropriate handler");
          }
          try {
            dstNode.updateVectorClock(message);
            handler.invoke(dstNode, message, src);
          } catch (InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
            throw new RuntimeException(
                "InvocationTargetException or IllegalAccessException in send");
          }

          retryTimer.cancel();
        }
      }
    };

    retryTimer.scheduleAtFixedRate(tryTransmit, RETRY_MILLIS, RETRY_MILLIS);
  }

  private Method getMessageHandler(Message message, Node node) {
    String handlerName = "handle" + message.getClass().getSimpleName();
    Class<? extends Node> dstNodeClass = node.getClass();
    try {
      return dstNodeClass.getMethod(handlerName, Message.class, Address.class);
    } catch (NoSuchMethodException e) {
      return null;
    }
  }
}
