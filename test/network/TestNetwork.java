package network;

import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import util.Pair;
import util.TestingMessage;
import util.TestingNetwork;
import util.TestingNode;

public class TestNetwork {

  private Map<Integer, Address> addresses;
  private Map<Integer, TestingNode> nodes;
  private Random random;

  @BeforeEach
  public void setup() {
    random = new Random();
  }

  public void setupNetwork(float txSuccessRate, List<Integer> addressInts) {
    TestingNetwork network = new TestingNetwork(txSuccessRate);
    addresses = new HashMap<>();
    nodes = new HashMap<>();
    for (Integer a : addressInts) {
      Address address = new Address(a);
      nodes.put(a, new TestingNode(address, network));
      addresses.put(a, address);
    }
  }

  @Test
  public synchronized void testSend() {
    int N = 10;
    int T = 2000;

    // Send a series of messages and confirm that they're all received
    setupNetwork(Network.RELIABLE_TX,
        IntStream.rangeClosed(1, N).boxed().collect(Collectors.toList()));

    Map<Integer, List<Pair<Message, Address>>> expectedReceivedMessages = new HashMap<>();

    for (int i = 1; i <= N; i++) {
      expectedReceivedMessages.put(i, new ArrayList<>());
    }

    for (int i = 0; i < T; i++) {
      int src = random.nextInt(1, N + 1);
      int dst = random.nextInt(1, N + 1);
      int data = random.nextInt();

      TestingMessage message = new TestingMessage(data);
      node(src).send(message, address(dst));
      expectedReceivedMessages.get(dst).add(new Pair<>(message, address(src)));
    }

    await().atMost(Duration.ofSeconds(2)).until(() -> {
      for (int i = 1; i <= N; i++) {
        for (Pair<Message, Address> expected : expectedReceivedMessages.get(i)) {
          if (!node(i).getReceivedMessages().contains(expected)) {
            return false;
          }
        }
      }
      return true;
    });
  }

  @Test
  public synchronized void testSendUnreliable() {
    int N = 10;
    int T = 10000;

    // Send a series of messages and confirm that they're all received
    setupNetwork(0.8f, IntStream.rangeClosed(1, N).boxed().collect(Collectors.toList()));

    Map<Integer, List<Pair<Message, Address>>> expectedReceivedMessages = new HashMap<>();

    for (int i = 1; i <= N; i++) {
      expectedReceivedMessages.put(i, new ArrayList<>());
    }

    for (int i = 0; i < T; i++) {
      int src = random.nextInt(1, N + 1);
      int dst = random.nextInt(1, N + 1);
      int data = random.nextInt();

      TestingMessage message = new TestingMessage(data);
      node(src).send(message, address(dst));
      expectedReceivedMessages.get(dst).add(new Pair<>(message, address(src)));
    }

    await().atMost(Duration.ofSeconds(5)).until(() -> {
      for (int i = 1; i <= N; i++) {
        for (Pair<Message, Address> expected : expectedReceivedMessages.get(i)) {
          if (!node(i).getReceivedMessages().contains(expected)) {
            return false;
          }
        }
      }
      return true;
    });
  }

  private TestingNode node(int a) {
    return nodes.getOrDefault(a, null);
  }

  private Address address(int a) {
    return addresses.getOrDefault(a, null);
  }
}
