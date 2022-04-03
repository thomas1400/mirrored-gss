package whiteboard;

import gss.GameEvent;
import gss.GameState;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;

public class WhiteboardState extends GameState {

  private final BufferedImage board;

  public WhiteboardState(Image board, int simTime) {
    this((BufferedImage) board, simTime);
  }

  public WhiteboardState(BufferedImage board, int simTime) {
    super(simTime);
    this.board = board;
  }

  public BufferedImage getBoard() {
    return this.board;
  }

  @Override
  public synchronized void applyEvent(GameEvent event) {
    if (!(event instanceof WhiteboardEvent delta)) {
      throw new IllegalArgumentException("WhiteboardState only accepts events of type WhiteboardEvent");
    }

    super.applyEvent(event); // ignore the results

    if (delta.getStart() != null || delta.getEnd() != null) {
      Graphics graphics = board.getGraphics();
      graphics.setColor(Color.black);
      graphics.drawLine(delta.getStart().x, delta.getStart().y, delta.getEnd().x, delta.getEnd().y);
    }
  }

  /**
   * Copy a buffered image. From https://stackoverflow.com/questions/3514158/how-do-you-clone-a-bufferedimage
   * @param bi image to copy
   * @return deep copy of input image
   */
  private static BufferedImage deepCopy(BufferedImage bi) {
    ColorModel cm = bi.getColorModel();
    boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
    WritableRaster raster = bi.copyData(null);
    return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
  }

  @Override
  public GameState copy() {
    return new WhiteboardState(deepCopy(board), getSimTime());
  }

  @Override
  public synchronized boolean equals(Object other) {
    if (!(other instanceof WhiteboardState ows)) {
      return false;
    }

    return this.getSimTime() == ows.getSimTime() && imagesAreEqual(board, ows.getBoard());
  }

  /**
   * Compares two images pixel by pixel.
   * From: https://stackoverflow.com/questions/11006394/is-there-a-simple-way-to-compare-bufferedimage-instances
   *
   * @param imgA the first image.
   * @param imgB the second image.
   * @return whether the images are both the same or not.
   */
  public static boolean imagesAreEqual(BufferedImage imgA, BufferedImage imgB) {
    // The images must be the same size.
    if (imgA.getWidth() != imgB.getWidth() || imgA.getHeight() != imgB.getHeight()) {
      return false;
    }

    int width  = imgA.getWidth();
    int height = imgA.getHeight();

    // Loop over every pixel.
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        // Compare the pixels for equality.
        if (imgA.getRGB(x, y) != imgB.getRGB(x, y)) {
          return false;
        }
      }
    }

    return true;
  }
}
