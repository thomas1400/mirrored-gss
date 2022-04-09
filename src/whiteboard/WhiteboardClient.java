package whiteboard;

import gss.GSSClient;
import gss.GameEventMessage;
import gss.GameStateMessage;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Component;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Label;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collection;

import network.Address;
import network.Message;
import network.Network;

import javax.swing.*;

public class WhiteboardClient extends GSSClient implements MouseListener, MouseMotionListener {

  private static final int HEARTBEAT_PERIOD_MS = 250;

  private WhiteboardState state;
  private Component whiteboard;
  private Point lastDrawPoint;
  private Timer heartbeatTimer;

  public WhiteboardClient(Address address, Address gss, Network network) {
    super(address, gss, network);

    buildUI();
  }

  // TODO : add heartbeats to clients to make fossil collection work without requiring
  //  every client to be sending updates. can just send null events on a timer

  public void startRunning() {
    heartbeatTimer = new Timer(HEARTBEAT_PERIOD_MS, e -> sendHeartbeat());
    heartbeatTimer.start();
  }

  private void sendHeartbeat() {
    this.send(new GameEventMessage(null, getAddress(), gss,
            state.getSimTime(), state.getGssTime(), getVectorClock()), gss);
  }

  public void stopRunning() {
    heartbeatTimer.stop();
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
    Label label1 = new java.awt.Label("Collaborative Whiteboard"); // FIXME add user ID?
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
    state = new WhiteboardState(buffer, 0);
    redraw();

    f.setResizable(false);
    f.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent we) {
        System.exit(0);
      }
    });
  }


  private void redraw() {
    whiteboard.getGraphics().drawImage(state.getBoard(), 0, 0, whiteboard);
  }

  private synchronized void drawDeltaFromMouseEvent(MouseEvent e) {
    Point currentPoint = e.getPoint();
    WhiteboardEvent delta = new WhiteboardEvent(lastDrawPoint, currentPoint,
        state.getSimTime() + 1);
    GameEventMessage message = new GameEventMessage(delta, this.getAddress(), gss, delta.getSimTime(),
        state.getGssTime(), getVectorClock());
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
//      System.out.println("exiting here bad sender");
      return;
    }
    if (!(m instanceof GameStateMessage gsm)) {
      throw new RuntimeException("Attempted to use handler for wrong kind of message");
    }
    if (!(gsm.getState() instanceof WhiteboardState state)) {
      throw new RuntimeException(
          "Mismatched state; WhiteboardClient can only handle WhiteboardState");
    }
    if (state.getSimTime() < this.state.getSimTime() || gsm.getGssTime() <= this.state.getGssTime()) {
      return; // it never makes sense to accept state with a lower sim time to our own
    }

//    System.out.printf("[client] addr %s received state (s.t. %d, g.t. %d)\n", this.getAddress(),
//      state.getSimTime(), gsm.getGssTime());

    this.state = (WhiteboardState) state.copy();

    redraw();
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

  public void mouseClicked(MouseEvent e) {
  }

  public void mouseEntered(MouseEvent e) {
  }

  public void mouseExited(MouseEvent e) {
  }

  public void mouseMoved(MouseEvent e) {
  }


  /*
   * Methods exposed for testing
   */
  public synchronized void acceptGameEvent(WhiteboardEvent event) {
    GameEventMessage message = new GameEventMessage(event, this.getAddress(), gss, event.getSimTime(),
        state.getGssTime(), getVectorClock());

    send(message, gss);
    this.state.applyEvent(event);

    redraw();
  }
}
