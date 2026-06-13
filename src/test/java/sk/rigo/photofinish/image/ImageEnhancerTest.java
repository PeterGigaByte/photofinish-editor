package sk.rigo.photofinish.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;

class ImageEnhancerTest {

  private final ImageEnhancer enhancer = new ImageEnhancer();

  @Test
  void respectsDisabledFlag() {
    BufferedImage image = gradient(64, 64);
    assertSame(image, enhancer.enhanceIfEnabled(image, false), "enhancement must be skipped when disabled");
  }

  @Test
  void keepsDimensionsAndBoostsContrast() {
    BufferedImage image = gradient(128, 64);

    BufferedImage enhanced = enhancer.enhanceIfEnabled(image, true);

    assertEquals(image.getWidth(), enhanced.getWidth());
    assertEquals(image.getHeight(), enhanced.getHeight());

    // A mid-tone below 128 should be pushed darker and one above 128 pushed lighter (more contrast).
    int darkBefore = new Color(image.getRGB(40, 32)).getRed();
    int darkAfter = new Color(enhanced.getRGB(40, 32)).getRed();
    int lightBefore = new Color(image.getRGB(110, 32)).getRed();
    int lightAfter = new Color(enhanced.getRGB(110, 32)).getRed();

    assertTrue(darkBefore < 128 && lightBefore > 128, "test sample should straddle mid-grey");
    assertTrue(darkAfter <= darkBefore, "tones below mid-grey should not get lighter");
    assertTrue(lightAfter >= lightBefore, "tones above mid-grey should not get darker");
    assertNotEquals(darkBefore + lightBefore, darkAfter + lightAfter, "contrast should change the pixels");
  }

  private static BufferedImage gradient(int width, int height) {
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = image.createGraphics();
    for (int x = 0; x < width; x++) {
      int v = (int) Math.round(255.0 * x / (width - 1));
      graphics.setColor(new Color(v, v, v));
      graphics.fillRect(x, 0, 1, height);
    }
    graphics.dispose();
    return image;
  }
}
