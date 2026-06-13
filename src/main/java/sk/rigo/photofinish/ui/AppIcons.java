package sk.rigo.photofinish.ui;

import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public final class AppIcons {

  private AppIcons() {
  }

  public static Image windowIcon() {
    int size = 256;
    Canvas canvas = new Canvas(size, size);
    GraphicsContext graphics = canvas.getGraphicsContext2D();

    graphics.setFill(Color.rgb(17, 21, 29));
    graphics.fillRoundRect(0, 0, size, size, 46, 46);

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

    SnapshotParameters parameters = new SnapshotParameters();
    parameters.setFill(Color.TRANSPARENT);
    return canvas.snapshot(parameters, null);
  }
}
