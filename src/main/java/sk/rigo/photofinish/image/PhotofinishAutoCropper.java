package sk.rigo.photofinish.image;

import java.awt.image.BufferedImage;
import java.util.Arrays;

/**
 * Trims the empty (no-participant) stretches from long photofinish strips while keeping the
 * original pixels. Only the width changes by the amount that is cropped away; nothing is scaled.
 *
 * <p>A photofinish image encodes time along its width: every column is a thin slice captured at
 * the finish line. Columns where no athlete is present look almost identical to the static
 * background (empty track / lane lines). When an athlete crosses, the affected columns differ
 * noticeably from that background. This class measures, per column, how far it deviates from the
 * background, keeps the span of "active" columns (plus a small margin) and removes the flat
 * leading and trailing runs.
 *
 * <p>The detector is intentionally conservative: it only engages for genuinely long strips, it
 * never upscales or distorts, and it returns the source unchanged whenever it cannot confidently
 * locate participants (for example a uniformly empty or already-tight image).
 */
public class PhotofinishAutoCropper {

  /** Only treat an image as a long strip worth cropping when it is at least this many times wider than tall. */
  private static final double MIN_LONG_ASPECT = 2.5;
  /** Upper bound on the number of rows sampled when measuring column activity (keeps very tall strips fast). */
  private static final int MAX_ROW_SAMPLES = 256;
  /** Upper bound on the number of columns sampled when estimating the per-row background. */
  private static final int MAX_BG_COLUMN_SAMPLES = 512;
  /** Fraction of the high activity level above the floor at which a column counts as containing a participant. */
  private static final double ACTIVITY_THRESHOLD_FRACTION = 0.15;
  /** Padding kept on each side of the detected participant span, as a fraction of the original width. */
  private static final double MARGIN_FRACTION = 0.02;

  /**
   * Returns an auto-cropped view of {@code source} when cropping is requested and beneficial,
   * otherwise the original image. The returned image always shares the original resolution of the
   * pixels it keeps (no scaling).
   */
  public BufferedImage cropIfBeneficial(BufferedImage source, boolean enabled) {
    if (!enabled || source == null) {
      return source;
    }
    int width = source.getWidth();
    int height = source.getHeight();
    if (width <= 1 || height <= 1 || (double) width / height < MIN_LONG_ASPECT) {
      return source;
    }

    int rowStep = Math.max(1, height / MAX_ROW_SAMPLES);
    int[] sampledRows = sampledIndices(height, rowStep);
    int[] background = backgroundLumaPerRow(source, sampledRows, width);

    double[] activity = new double[width];
    double max = 0.0;
    double min = Double.MAX_VALUE;
    for (int x = 0; x < width; x++) {
      long sum = 0;
      for (int i = 0; i < sampledRows.length; i++) {
        sum += Math.abs(luma(source.getRGB(x, sampledRows[i])) - background[i]);
      }
      double value = (double) sum / sampledRows.length;
      activity[x] = value;
      max = Math.max(max, value);
      min = Math.min(min, value);
    }

    double threshold = min + (max - min) * ACTIVITY_THRESHOLD_FRACTION;
    int firstActive = -1;
    int lastActive = -1;
    for (int x = 0; x < width; x++) {
      if (activity[x] >= threshold) {
        if (firstActive < 0) {
          firstActive = x;
        }
        lastActive = x;
      }
    }

    if (firstActive < 0) {
      // No participant detected anywhere (e.g. a blank strip) - leave the image untouched.
      return source;
    }

    int margin = (int) Math.round(width * MARGIN_FRACTION);
    int x0 = Math.max(0, firstActive - margin);
    int x1 = Math.min(width - 1, lastActive + margin);
    int croppedWidth = x1 - x0 + 1;
    if (croppedWidth >= width) {
      return source;
    }
    return source.getSubimage(x0, 0, croppedWidth, height);
  }

  private static int[] backgroundLumaPerRow(BufferedImage source, int[] sampledRows, int width) {
    int columnStep = Math.max(1, width / MAX_BG_COLUMN_SAMPLES);
    int[] sampledColumns = sampledIndices(width, columnStep);
    int[] background = new int[sampledRows.length];
    int[] columnLuma = new int[sampledColumns.length];
    for (int i = 0; i < sampledRows.length; i++) {
      int y = sampledRows[i];
      for (int j = 0; j < sampledColumns.length; j++) {
        columnLuma[j] = luma(source.getRGB(sampledColumns[j], y));
      }
      background[i] = median(columnLuma);
    }
    return background;
  }

  private static int[] sampledIndices(int size, int step) {
    int count = (size + step - 1) / step;
    int[] indices = new int[count];
    for (int i = 0; i < count; i++) {
      indices[i] = Math.min(size - 1, i * step);
    }
    return indices;
  }

  private static int median(int[] values) {
    int[] copy = Arrays.copyOf(values, values.length);
    Arrays.sort(copy);
    return copy[copy.length / 2];
  }

  private static int luma(int argb) {
    int r = (argb >> 16) & 0xFF;
    int g = (argb >> 8) & 0xFF;
    int b = argb & 0xFF;
    return (int) Math.round(0.299 * r + 0.587 * g + 0.114 * b);
  }
}
