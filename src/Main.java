import gss.GSS;
import gss.GameState;
import network.Address;
import network.Network;
import whiteboard.WhiteboardClient;

public class Main {
  public static void main(String[] args) {
    Network network = new Network(0.8f);
    GSS gss = new GSS(new Address(1), network);
    GSS gss2 = new GSS(new Address(4), network);
    WhiteboardClient client1 = new WhiteboardClient(new Address(2), gss.getAddress(), network);
    WhiteboardClient client2 = new WhiteboardClient(new Address(3), gss2.getAddress(), network);
    GameState initialState = client1.getState().copy();
    gss.setState(initialState);
    gss.addClient(client1);
    gss2.setState(initialState.copy());
    gss2.addClient(client2);

    gss.startRunning();
    gss2.startRunning();
  }
}
