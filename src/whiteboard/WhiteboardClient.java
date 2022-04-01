package whiteboard;

import gss.GSSClient;
import gss.GameEventMessage;
import gss.GameStateMessage;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import javax.swing.Timer;
import network.Address;
import network.Message;
import network.Network;

public class WhiteboardClient extends GSSClient implements MouseListener, MouseMotionListener {

  private Component whiteboard;
  private WhiteboardState state;
  private Point lastDrawPoint;
  private Instant lastTimeUpdate;
  private Address gss;

  public WhiteboardClient(Address address, Address gss, Network network) {
    super(address, gss, network);

    this.gss = gss;

    buildUI();
  }

  private void buildUI() {
    Frame f = new Frame();
    GridBagLayout gridbag = new GridBagLayout();
    GridBagConstraints c = new GridBagConstraints();
    f.setLayout(gridbag);
    c.fill = GridBagConstraints.BOTH;
    c.gridwidth = GridBagConstraints.REMAINDER;
    Canvas canvas1 = new java.awt.Canvas();
    canvas1.setSize(360, 280);
    canvas1.setBackground(Color.white);
    gridbag.setConstraints(canvas1, c);
    f.add(canvas1);
    Label label1 = new java.awt.Label("Collaborative Whiteboard"); // FIXME
    label1.setSize(100, 30);
    label1.setAlignment(Label.CENTER);
    gridbag.setConstraints(label1, c);
    f.add(label1);
    f.setSize(360, 350);
    f.setVisible(true);
    whiteboard = canvas1;
    whiteboard.addMouseListener(this);
    whiteboard.addMouseMotionListener(this);
    Image buffer = whiteboard.createImage(f.getSize().width, f.getSize().height);
    state = new WhiteboardState(buffer, 0); // FIXME sim time
    redraw();

    f.setResizable(false);
    f.addWindowListener(new WindowAdapter(){
      public void windowClosing(WindowEvent we)
      {
        System.exit(0);
      }
    });

    lastTimeUpdate = Instant.now();
//    startSimTimeUpdates();
  }

  private void startSimTimeUpdates() {
    Timer t = new Timer(10, //will run every 10 ms //FIXME factor out magic constant
        e -> {
          Instant now = Instant.now();
          float timeDelta = ChronoUnit.MILLIS.between(lastTimeUpdate, now);
          state.applyEvent(new WhiteboardEvent(null, null, state.getSimTime() + timeDelta));
          lastTimeUpdate = now;
        });
    t.start();
  }

  private void redraw() {
    whiteboard.getGraphics().drawImage(state.getBoard(), 0, 0, whiteboard);
  }

  private synchronized void drawDeltaFromMouseEvent(MouseEvent e) {
    Point currentPoint = e.getPoint();
    WhiteboardEvent delta = new WhiteboardEvent(lastDrawPoint, currentPoint, state.getSimTime()+1);
    GameEventMessage message = new GameEventMessage(delta, this.getAddress());
    lastDrawPoint = currentPoint;

    send(message, gss);
    this.state.applyEvent(delta);

    redraw();
  }

  public synchronized WhiteboardState getState() {
    return this.state;
  }

  /*
   * Message Handlers
   */
  public synchronized void handleGameStateMessage(Message m, Address sender) {
    if (!this.gss.equals(sender)) {
      return;
    }
    if (!(m instanceof GameStateMessage gsm)) {
      throw new RuntimeException("Attempted to use handler for wrong kind of message");
    }
    if (!(gsm.state() instanceof WhiteboardState state)) {
      throw new RuntimeException("Mismatched state; WhiteboardClient can only handle WhiteboardState");
    }

    if (state.getSimTime() <= this.state.getSimTime()) {
      return; // it never makes sense to accept state with a lower/equal sim time to our own
    }

    System.out.printf("[client] addr %s received state (s.t. %f)\n", this.getAddress(), state.getSimTime());

    this.state = state; // TODO : rollback protocol here
    redraw();
//    this.whiteboard.requestFocusInWindow();
  }


  @Override
  public void mousePressed(MouseEvent e) {
    lastDrawPoint = e.getPoint();
  }

  @Override
  public void mouseDragged(MouseEvent e) {
    drawDeltaFromMouseEvent(e);
  }

  @Override
  public void mouseReleased(MouseEvent e) {
    drawDeltaFromMouseEvent(e);
  }

  public void mouseClicked(MouseEvent e) { }
  public void mouseEntered(MouseEvent e) { }
  public void mouseExited(MouseEvent e) { }
  public void mouseMoved(MouseEvent e) { }


  /*
   * Methods exposed for testing
   */
  public synchronized void acceptTestingEvent(WhiteboardEvent event) {
    GameEventMessage message = new GameEventMessage(event, this.getAddress());

    send(message, gss);
    this.state.applyEvent(event);

    redraw();
  }
}
