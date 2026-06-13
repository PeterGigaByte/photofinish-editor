package sk.rigo.photofinish.image;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;

/**
 * Applies a light, broadly-flattering retouch to a photo before it is placed on the poster: a mild
 * sharpen to crisp up the athletes, plus a small contrast and saturation lift so the strip does not
 * look flat. The amounts are deliberately subtle - the goal is "a bit nicer", not an obviously
 * processed look. Nothing is scaled and the dimensions are preserved.
 */
public class ImageEnhancer {

  /** Strength of the 3x3 sharpen kernel (higher = crisper, more haloing). */
  private static final float SHARPEN_AMOUNT = 0.15f;
  /** Contrast multiplier applied around mid-grey. */
  private static final double CONTRAST = 1.06;
  /** Saturation multiplier (1.0 = unchanged). */
  private static final double SATURATION = 1.10;

  /**
   * Returns an enhanced copy of {@code source} when enabled, otherwise the original image. The
   * result always has the same dimensions as the input.
   */
  public BufferedImage enhanceIfEnabled(BufferedImage source, boolean enabled) {
    if (!enabled || source == null) {
      return source;
    }
    int width = source.getWidth();
    int height = source.getHeight();
    if (width <= 2 || height <= 2) {
      return source;
    }

    int type = source.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;

    // 1) Mild sharpen via a native convolution. The kernel sums to 1 so overall brightness is kept.
    float k = SHARPEN_AMOUNT;
    float[] kernel = {
        0f, -k, 0f,
        -k, 1f + 4f * k, -k,
        0f, -k, 0f
    };
    ConvolveOp sharpen = new ConvolveOp(new Kernel(3, 3, kernel), ConvolveOp.EDGE_NO_OP, null);
    BufferedImage result = sharpen.filter(toType(source, type), new BufferedImage(width, height, type));

    // 2) Contrast + saturation in a single pixel pass.
    int[] pixels = result.getRGB(0, 0, width, height, null, 0, width);
    for (int i = 0; i < pixels.length; i++) {
      int argb = pixels[i];
      int a = (argb >>> 24) & 0xFF;
      int r = (argb >> 16) & 0xFF;
      int g = (argb >> 8) & 0xFF;
      int b = argb & 0xFF;

      r = clamp((int) Math.round((r - 128) * CONTRAST + 128));
      g = clamp((int) Math.round((g - 128) * CONTRAST + 128));
      b = clamp((int) Math.round((b - 128) * CONTRAST + 128));

      double gray = 0.299 * r + 0.587 * g + 0.114 * b;
      r = clamp((int) Math.round(gray + (r - gray) * SATURATION));
      g = clamp((int) Math.round(gray + (g - gray) * SATURATION));
      b = clamp((int) Math.round(gray + (b - gray) * SATURATION));

      pixels[i] = (a << 24) | (r << 16) | (g << 8) | b;
    }
    result.setRGB(0, 0, width, height, pixels, 0, width);
    return result;
  }

  private static BufferedImage toType(BufferedImage source, int type) {
    if (source.getType() == type) {
      return source;
    }
    BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), type);
    Graphics2D graphics = copy.createGraphics();
    try {
      graphics.drawImage(source, 0, 0, null);
    } finally {
      graphics.dispose();
    }
    return copy;
  }

  private static int clamp(int value) {
    return value < 0 ? 0 : Math.min(value, 255);
  }
}
