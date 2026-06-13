package sk.rigo.photofinish.image;

import java.awt.Graphics2D;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Trims the empty (no-participant) stretches from long photofinish strips while keeping the
 * original pixels. Only the width changes by the amount that is cropped away; nothing is scaled.
 *
 * <p>A photofinish image encodes time along its width: every column is a thin slice captured at
 * the finish line. Columns where no athlete is present look almost identical to the static
 * background (empty track / lane lines). When an athlete crosses, the affected columns differ
 * noticeably from that background. This class measures, per column, how far it deviates from the
 * background, groups the "active" columns into participant clusters and removes the flat runs at
 * the front, at the back and between participants. Each kept cluster retains a safety margin of
 * real background on both sides, so no athlete is ever clipped and neighbouring participants stay
 * visually separated.
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
  /** Percentile of column activity treated as the empty-background level. */
  private static final double EMPTY_PERCENTILE = 0.10;
  /** Near-top percentile used as the "busy" reference (robust to a single bright outlier column). */
  private static final double CONTENT_PERCENTILE = 0.99;
  /**
   * How far above the empty level (as a fraction of the empty→busy span) a column must be to count as
   * containing a participant. Deliberately low so faint athletes are kept, never trimmed.
   */
  private static final double CONTENT_THRESHOLD_FRACTION = 0.06;
  /** Absolute minimum activity above background (summed over R+G+B) before a column counts as content. */
  private static final double MIN_ACTIVITY_DELTA = 6.0;
  /** Padding of real background kept on each side of every participant cluster, as a fraction of width. */
  private static final double MARGIN_FRACTION = 0.03;
  /** Empty gaps narrower than this fraction of the width are kept intact, never collapsed. */
  private static final double MIN_REMOVABLE_GAP_FRACTION = 0.08;

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
    int[][] background = backgroundColorPerRow(source, sampledRows, width);

    // Per-column activity = how far the column's colour differs from the static background, summed
    // over R+G+B. Using colour (not just brightness) catches athletes whose jersey matches the
    // track brightness but differs in hue, so they are not mistaken for empty track.
    double[] activity = new double[width];
    for (int x = 0; x < width; x++) {
      long sum = 0;
      for (int i = 0; i < sampledRows.length; i++) {
        int rgb = source.getRGB(x, sampledRows[i]);
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        sum += Math.abs(r - background[i][0]) + Math.abs(g - background[i][1]) + Math.abs(b - background[i][2]);
      }
      activity[x] = (double) sum / sampledRows.length;
    }

    // Robust threshold: compare against percentiles so a single bright feature cannot inflate the
    // scale and push real (fainter) participants below the line. The bar is deliberately low and
    // biased towards KEEPING columns, so participants are never trimmed.
    double[] sorted = activity.clone();
    Arrays.sort(sorted);
    double emptyLevel = percentile(sorted, EMPTY_PERCENTILE);
    double contentLevel = percentile(sorted, CONTENT_PERCENTILE);
    double threshold = emptyLevel
        + Math.max(MIN_ACTIVITY_DELTA, (contentLevel - emptyLevel) * CONTENT_THRESHOLD_FRACTION);

    // Group active columns into participant clusters, ignoring 1-2px noise so a stray pixel does
    // not pin an otherwise empty region as "kept".
    int minClusterWidth = Math.max(2, width / 600);
    List<int[]> clusters = new ArrayList<>();
    int runStart = -1;
    for (int x = 0; x < width; x++) {
      if (activity[x] >= threshold) {
        if (runStart < 0) {
          runStart = x;
        }
      } else if (runStart >= 0) {
        if (x - runStart >= minClusterWidth) {
          clusters.add(new int[]{runStart, x - 1});
        }
        runStart = -1;
      }
    }
    if (runStart >= 0 && width - runStart >= minClusterWidth) {
      clusters.add(new int[]{runStart, width - 1});
    }

    if (clusters.isEmpty()) {
      // No participant detected anywhere (e.g. a blank strip) - leave the image untouched.
      return source;
    }

    // Keep small gaps intact: a short low-activity stretch may be a low-contrast part of an athlete
    // (between arms/legs), so only genuinely large empty runs are eligible for removal.
    int minRemovableGap = Math.max(2 * (int) Math.round(width * MARGIN_FRACTION),
        (int) Math.round(width * MIN_REMOVABLE_GAP_FRACTION));
    List<int[]> bridged = new ArrayList<>();
    for (int[] cluster : clusters) {
      if (!bridged.isEmpty() && cluster[0] - bridged.get(bridged.size() - 1)[1] - 1 < minRemovableGap) {
        bridged.get(bridged.size() - 1)[1] = cluster[1];
      } else {
        bridged.add(new int[]{cluster[0], cluster[1]});
      }
    }

    // Expand every cluster by a safety margin of real background on both sides, then merge ranges
    // that touch or overlap. Concatenating the kept ranges removes the empty space at the front, the
    // back and between participants, while the margin guarantees no athlete is clipped and adjacent
    // participants keep a visible buffer between them.
    int margin = Math.max(12, (int) Math.round(width * MARGIN_FRACTION));
    List<int[]> keep = new ArrayList<>();
    for (int[] cluster : bridged) {
      int start = Math.max(0, cluster[0] - margin);
      int end = Math.min(width - 1, cluster[1] + margin);
      if (!keep.isEmpty() && start <= keep.get(keep.size() - 1)[1] + 1) {
        keep.get(keep.size() - 1)[1] = Math.max(keep.get(keep.size() - 1)[1], end);
      } else {
        keep.add(new int[]{start, end});
      }
    }

    int keptWidth = 0;
    for (int[] range : keep) {
      keptWidth += range[1] - range[0] + 1;
    }
    if (keptWidth >= width) {
      // Participants span the whole strip - nothing meaningful to remove.
      return source;
    }

    int type = source.getTransparency() == Transparency.OPAQUE
        ? BufferedImage.TYPE_INT_RGB
        : BufferedImage.TYPE_INT_ARGB;
    BufferedImage cropped = new BufferedImage(keptWidth, height, type);
    Graphics2D graphics = cropped.createGraphics();
    try {
      int destX = 0;
      for (int[] range : keep) {
        int rangeWidth = range[1] - range[0] + 1;
        graphics.drawImage(source.getSubimage(range[0], 0, rangeWidth, height), destX, 0, null);
        destX += rangeWidth;
      }
    } finally {
      graphics.dispose();
    }
    return cropped;
  }

  private static int[][] backgroundColorPerRow(BufferedImage source, int[] sampledRows, int width) {
    int columnStep = Math.max(1, width / MAX_BG_COLUMN_SAMPLES);
    int[] sampledColumns = sampledIndices(width, columnStep);
    int[][] background = new int[sampledRows.length][3];
    int[] reds = new int[sampledColumns.length];
    int[] greens = new int[sampledColumns.length];
    int[] blues = new int[sampledColumns.length];
    for (int i = 0; i < sampledRows.length; i++) {
      int y = sampledRows[i];
      for (int j = 0; j < sampledColumns.length; j++) {
        int rgb = source.getRGB(sampledColumns[j], y);
        reds[j] = (rgb >> 16) & 0xFF;
        greens[j] = (rgb >> 8) & 0xFF;
        blues[j] = rgb & 0xFF;
      }
      background[i][0] = median(reds);
      background[i][1] = median(greens);
      background[i][2] = median(blues);
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

  private static double percentile(double[] sortedAscending, double quantile) {
    if (sortedAscending.length == 0) {
      return 0.0;
    }
    int index = (int) Math.round(quantile * (sortedAscending.length - 1));
    index = Math.max(0, Math.min(sortedAscending.length - 1, index));
    return sortedAscending[index];
  }
}
