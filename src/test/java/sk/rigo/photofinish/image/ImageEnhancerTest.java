package sk.rigo.photofinish.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    BufferedImage image = grayGradient(64, 64, 0, 255);
    assertSame(image, enhancer.enhanceIfEnabled(image, false), "enhancement must be skipped when disabled");
  }

  @Test
  void autoLevelsStretchesACompressedTonalRange() {
    // A hazy image whose tones only span 50..150. Auto-levels should expand the range using the
    // full 0..255 scale, based purely on the image's own histogram.
    int width = 128;
    int height = 64;
    BufferedImage image = grayGradient(width, height, 50, 150);

    BufferedImage enhanced = enhancer.enhanceIfEnabled(image, true);

    assertEquals(width, enhanced.getWidth());
    assertEquals(height, enhanced.getHeight());

    int leftBefore = new Color(image.getRGB(2, 32)).getRed(); // ~50
    int rightBefore = new Color(image.getRGB(width - 3, 32)).getRed(); // ~150
    int leftAfter = new Color(enhanced.getRGB(2, 32)).getRed();
    int rightAfter = new Color(enhanced.getRGB(width - 3, 32)).getRed();

    assertTrue(leftAfter < leftBefore, "the dark end should be pulled down towards black");
    assertTrue(
        (rightAfter - leftAfter) > (rightBefore - leftBefore),
        "the tonal range should be wider after auto-levels");
  }

  @Test
  void leavesAFlatImageBasicallyUnchanged() {
    // A near-uniform image must not be crushed: with too small a tonal range, auto-levels is skipped.
    BufferedImage flat = grayGradient(64, 64, 120, 128);

    BufferedImage enhanced = enhancer.enhanceIfEnabled(flat, true);

    int before = new Color(flat.getRGB(32, 32)).getRed();
    int after = new Color(enhanced.getRGB(32, 32)).getRed();
    assertTrue(Math.abs(after - before) <= 12, "a flat image must not be crushed to black");
  }

  private static BufferedImage grayGradient(int width, int height, int min, int max) {
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = image.createGraphics();
    for (int x = 0; x < width; x++) {
      int v = min + (int) Math.round((double) (max - min) * x / (width - 1));
      graphics.setColor(new Color(v, v, v));
      graphics.fillRect(x, 0, 1, height);
    }
    graphics.dispose();
    return image;
  }
}
