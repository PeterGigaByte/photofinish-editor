package sk.rigo.photofinish.image;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;

/**
 * Applies an automatic, content-aware retouch to a photo before it is placed on the poster. The
 * amounts are derived from the image itself - there are no fixed knobs:
 *
 * <ul>
 *   <li><b>Auto-levels:</b> the black/white points are read from the luminance histogram (robust
 *       low/high percentiles) and the tones are stretched to use the full range, so flat, hazy
 *       strips get contrast while already well-exposed ones are barely touched.</li>
 *   <li><b>Adaptive saturation:</b> dull images get a larger colour boost, vivid ones almost none.</li>
 *   <li><b>Mild sharpen:</b> a small, fixed unsharp-style kernel to crisp up the athletes.</li>
 * </ul>
 *
 * <p>The retouch is deliberately gentle (the contrast stretch is capped) so it never looks
 * processed. Dimensions are always preserved.
 */
public class ImageEnhancer {

  private static final int HISTOGRAM_SAMPLES = 200_000;
  private static final double BLACK_PERCENTILE = 0.005;
  private static final double WHITE_PERCENTILE = 0.995;
  /** Never stretch contrast more than this, so the retouch stays subtle. */
  private static final double MAX_CONTRAST_STRETCH = 1.5;
  /** If the tonal range is narrower than this, skip auto-levels (the image is flat - stretching would crush it). */
  private static final double MIN_LEVELS_RANGE = 16.0;
  private static final float SHARPEN_AMOUNT = 0.12f;

  /**
   * Returns an automatically enhanced copy of {@code source} when enabled, otherwise the original
   * image. The enhancement parameters are computed from {@code source}. Dimensions are preserved.
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

    // Read the image's own statistics to decide how much to correct.
    Stats stats = analyse(source, width, height);
    double range = stats.whitePoint - stats.blackPoint;
    // Skip auto-levels on flat/near-uniform images, where stretching would crush them.
    double black = range < MIN_LEVELS_RANGE ? 0.0 : stats.blackPoint;
    double contrastScale = range < MIN_LEVELS_RANGE ? 1.0 : Math.min(MAX_CONTRAST_STRETCH, 255.0 / range);
    double saturation = adaptiveSaturation(stats.meanSaturation);

    // 1) Mild sharpen via a native convolution (kernel sums to 1, so brightness is preserved).
    float k = SHARPEN_AMOUNT;
    float[] kernel = {
        0f, -k, 0f,
        -k, 1f + 4f * k, -k,
        0f, -k, 0f
    };
    ConvolveOp sharpen = new ConvolveOp(new Kernel(3, 3, kernel), ConvolveOp.EDGE_NO_OP, null);
    BufferedImage result = sharpen.filter(toType(source, type), new BufferedImage(width, height, type));

    // 2) Auto-levels + adaptive saturation in a single pixel pass.
    int[] pixels = result.getRGB(0, 0, width, height, null, 0, width);
    for (int i = 0; i < pixels.length; i++) {
      int argb = pixels[i];
      int a = (argb >>> 24) & 0xFF;
      int r = (argb >> 16) & 0xFF;
      int g = (argb >> 8) & 0xFF;
      int b = argb & 0xFF;

      r = clamp((int) Math.round((r - black) * contrastScale));
      g = clamp((int) Math.round((g - black) * contrastScale));
      b = clamp((int) Math.round((b - black) * contrastScale));

      if (saturation != 1.0) {
        double gray = 0.299 * r + 0.587 * g + 0.114 * b;
        r = clamp((int) Math.round(gray + (r - gray) * saturation));
        g = clamp((int) Math.round(gray + (g - gray) * saturation));
        b = clamp((int) Math.round(gray + (b - gray) * saturation));
      }

      pixels[i] = (a << 24) | (r << 16) | (g << 8) | b;
    }
    result.setRGB(0, 0, width, height, pixels, 0, width);
    return result;
  }

  private record Stats(double blackPoint, double whitePoint, double meanSaturation) {
  }

  private static Stats analyse(BufferedImage source, int width, int height) {
    long total = (long) width * height;
    int step = (int) Math.max(1, Math.sqrt((double) total / HISTOGRAM_SAMPLES));
    int[] histogram = new int[256];
    int count = 0;
    double saturationSum = 0.0;
    for (int y = 0; y < height; y += step) {
      for (int x = 0; x < width; x += step) {
        int rgb = source.getRGB(x, y);
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        int luma = (int) Math.round(0.299 * r + 0.587 * g + 0.114 * b);
        histogram[Math.max(0, Math.min(255, luma))]++;

        int max = Math.max(r, Math.max(g, b));
        int min = Math.min(r, Math.min(g, b));
        saturationSum += max == 0 ? 0.0 : (double) (max - min) / max;
        count++;
      }
    }
    double blackPoint = percentileFromHistogram(histogram, count, BLACK_PERCENTILE);
    double whitePoint = percentileFromHistogram(histogram, count, WHITE_PERCENTILE);
    double meanSaturation = count == 0 ? 0.0 : saturationSum / count;
    return new Stats(blackPoint, whitePoint, meanSaturation);
  }

  private static double percentileFromHistogram(int[] histogram, int count, double quantile) {
    if (count == 0) {
      return quantile < 0.5 ? 0 : 255;
    }
    long target = (long) Math.floor(quantile * count);
    long running = 0;
    for (int level = 0; level < histogram.length; level++) {
      running += histogram[level];
      if (running >= target) {
        return level;
      }
    }
    return 255;
  }

  /** Dull images (low mean saturation) get a stronger colour boost; already-vivid ones almost none. */
  private static double adaptiveSaturation(double meanSaturation) {
    if (meanSaturation < 0.20) {
      return 1.20;
    }
    if (meanSaturation < 0.45) {
      return 1.10;
    }
    return 1.03;
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
