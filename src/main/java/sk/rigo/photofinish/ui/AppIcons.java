package sk.rigo.photofinish.ui;

import java.util.List;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public final class AppIcons {

  /** The icon is designed on a 256x256 grid and scaled to each requested size. */
  private static final double DESIGN_SIZE = 256.0;
  private static final int[] ICON_SIZES = {16, 24, 32, 48, 64, 128, 256};

  private AppIcons() {
  }

  /**
   * Window/taskbar icons rendered at several native sizes. Providing each size separately keeps the
   * icon crisp in the title bar and taskbar, where a single large image would be downscaled and look
   * blurry.
   */
  public static List<Image> windowIcons() {
    return java.util.Arrays.stream(ICON_SIZES).mapToObj(AppIcons::renderIcon).toList();
  }

  /** Largest icon, for callers that want a single image. */
  public static Image windowIcon() {
    return renderIcon(256);
  }

  private static Image renderIcon(int size) {
    Canvas canvas = new Canvas(size, size);
    GraphicsContext graphics = canvas.getGraphicsContext2D();
    double scale = size / DESIGN_SIZE;
    graphics.scale(scale, scale);
    draw(graphics);

    SnapshotParameters parameters = new SnapshotParameters();
    parameters.setFill(Color.TRANSPARENT);
    Image image = canvas.snapshot(parameters, null);
    return image;
  }

  /** Draws the "PF" photofinish badge on the 256x256 design grid. */
  private static void draw(GraphicsContext graphics) {
    graphics.setFill(Color.rgb(17, 21, 29));
    graphics.fillRoundRect(0, 0, 256, 256, 46, 46);

    graphics.setFill(Color.rgb(63, 182, 255));
    graphics.fillRoundRect(22, 24, 212, 70, 18, 18);

    graphics.setFill(Color.WHITE);
    graphics.setFont(Font.font("Segoe UI", FontWeight.BOLD, 58));
    graphics.fillText("PF", 52, 78);

    graphics.setStroke(Color.rgb(231, 238, 247, 0.74));
    graphics.setLineWidth(5);
    for (int y = 126; y <= 194; y += 24) {
      graphics.strokeLine(34, y, 222, y);
    }

    graphics.setFill(Color.rgb(168, 240, 122));
    graphics.fillRoundRect(42, 146, 172, 17, 8, 8);

    graphics.setStroke(Color.rgb(242, 67, 67));
    graphics.setLineWidth(8);
    graphics.strokeLine(150, 106, 150, 222);
  }
}
