package whiteboard;

import gss.GSSClient;
import gss.GameEventMessage;
import gss.GameStateMessage;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
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
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.imageio.ImageIO;
import network.Address;
import network.Message;
import network.Network;

import javax.swing.*;

public class WhiteboardClient extends GSSClient implements MouseListener, MouseMotionListener {

  private static final int HEARTBEAT_PERIOD_MS = 250;

  private Frame frame;
  private WhiteboardState state;
  private Component whiteboard;
  private Point lastDrawPoint;
  private Timer heartbeatTimer;
  private Image turtleSprite;
  private Point turtleLocation;

  public WhiteboardClient(Address address, Address gss, Network network) {
    super(address, gss, network);

    buildUI();

    try {
      turtleSprite = ImageIO.read(new File("images/turtle.png"));
    } catch (IOException e) {
      turtleSprite = null;
    }
  }

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
    frame = new Frame();
    GridBagLayout gridbag = new GridBagLayout();
    GridBagConstraints c = new GridBagConstraints();
    frame.setLayout(gridbag);
    c.fill = GridBagConstraints.BOTH;
    c.gridwidth = GridBagConstraints.REMAINDER;
    Canvas canvas1 = new Canvas();
    canvas1.setSize(360, 280);
    canvas1.setBackground(Color.white);
    gridbag.setConstraints(canvas1, c);
    frame.add(canvas1);
    Label label1 = new Label("Collaborative Whiteboard"); // TODO add user ID?
    label1.setSize(100, 30);
    label1.setAlignment(Label.CENTER);
    gridbag.setConstraints(label1, c);
    frame.add(label1);
    frame.setSize(360, 350);
    frame.setVisible(true);
    whiteboard = canvas1;
    whiteboard.addMouseListener(this);
    whiteboard.addMouseMotionListener(this);
    Image buffer = whiteboard.createImage(canvas1.getSize().width, canvas1.getSize().height);
    state = new WhiteboardState(buffer, 0);
    redraw();

    frame.setResizable(false);
    frame.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent we) {
        System.exit(0);
      }
    });
  }


  private void redraw() {
    whiteboard.getGraphics().drawImage(state.getBoard(), 0, 0, whiteboard);
    if (turtleLocation != null && turtleSprite != null) {
      whiteboard.getGraphics().drawImage(turtleSprite, turtleLocation.x, turtleLocation.y, null);
    }
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
    if (gsm.getGssTime() <= this.state.getGssTime()) { // state.getSimTime() < this.state.getSimTime() ||
      return; // it never makes sense to accept state with a lower sim time to our own
    }

//    System.out.printf("[client] addr %s received state (s.t. %d, g.t. %d)\n", this.getAddress(),
//      state.getSimTime(), gsm.getGssTime());

    this.state = (WhiteboardState) state.copy();

    for (Message unacked : unacknowledgedMessages) {
      if (unacked instanceof GameEventMessage gem) {
        if (gem.getEvent() instanceof WhiteboardEvent event) {
          this.state.applyEvent(event);
        }
      }
    }

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

  public void moveWindow(int windowIndex, int r, int c) {
    int rOffset = frame.getSize().height + 50;
    int cOffset = frame.getSize().width + 50;
    int wr = windowIndex / c;
    int wc = windowIndex % c;
    frame.setLocation(cOffset * wc, rOffset * wr);
  }

  public void drawTurtleAt(Point point) {
    if (turtleSprite != null) {
      int width = turtleSprite.getWidth(null);
      int height = turtleSprite.getHeight(null);
      turtleLocation = new Point(point.x - width / 2, point.y - height / 2);
    }
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
