package whiteboard;

import gss.GameEvent;
import java.awt.Point;
import java.util.Collection;

public class WhiteboardEvent extends GameEvent {

  private final Point start;
  private final Point end;

  public WhiteboardEvent(Point start, Point end, int simTime) {
    super(simTime);
    this.start = start;
    this.end = end;
  }

  public Point getStart() {
    return start;
  }

  public Point getEnd() {
    return end;
  }
}
