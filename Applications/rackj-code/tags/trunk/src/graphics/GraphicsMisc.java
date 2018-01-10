package graphics;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.swing.JPanel;

/**
 * This class gives some miscellaneous graphics-related methods and fields.
 * @author Wen-Dar Lin
 * @version 0.5
 */
public class GraphicsMisc {
  /**
   * Useful Color array.
   */
  public final static Color[] feasibleColors = {
      Color.black,
      Color.blue,
      Color.cyan,
      Color.gray,
      Color.green,
      Color.lightGray,
      Color.magenta,
      Color.orange,
      Color.pink,
      Color.red,
      Color.white,
  };

  /**
   * 1,000.
   */
  public final static int oneK = 1000;
  /**
   * 1,000,000.
   */
  public final static int oneM = oneK*oneK;

  /**
   * Number of points on the screen for a visible gap.
   */
  public final static int visibleGap = 5;
  /**
   * Number of points on the screen for a minor visible gap.
   */
  public final static int minorVisibleGap = 2;

  /**
   * Save a JPanel content to a file.
   * @param panel the JPanel object
   * @param file File object of the file to be written
   * @param typeStr specified image file type
   * @throws IOException if file-writing error occurs
   */
  public static void savePanel(JPanel panel,File file,String typeStr) throws IOException {
    BufferedImage image = new BufferedImage(panel.getWidth(),
                                            panel.getHeight(),
                                            BufferedImage.TYPE_INT_RGB);
    Graphics2D g2 = image.createGraphics();
    panel.paint(g2);
    javax.imageio.ImageIO.write(image, typeStr, file);
  }
}
