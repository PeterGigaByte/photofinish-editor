package sk.rigo.photofinish.image;

import sk.rigo.photofinish.model.BrandingTemplate;
import sk.rigo.photofinish.model.BrandingMetadata;
import sk.rigo.photofinish.model.HeaderFade;
import sk.rigo.photofinish.model.ImageFitMode;
import sk.rigo.photofinish.model.LogoPosition;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class BrandingRenderer {

  private final TextTemplateEngine textTemplateEngine = new TextTemplateEngine();
  private final PhotofinishAutoCropper autoCropper = new PhotofinishAutoCropper();
  private final ImageEnhancer imageEnhancer = new ImageEnhancer();

  /** Resolved geometry of the poster: output dimensions, header/results band heights and the photo rectangle. */
  private record PosterLayout(int width, int height, int headerHeight, int resultsHeight, Rectangle imageArea) {
  }

  public BufferedImage render(Path sourcePath, BrandingTemplate template) throws IOException {
    return render(sourcePath, template, BrandingMetadata.empty());
  }

  public BufferedImage render(Path sourcePath, BrandingTemplate template, BrandingMetadata metadata) throws IOException {
    BufferedImage source = ImageIO.read(sourcePath.toFile());
    if (source == null) {
      throw new IOException("Unsupported image file: " + sourcePath);
    }

    return render(source, sourcePath, template, metadata);
  }

  public BufferedImage render(BufferedImage source, Path sourcePath, BrandingTemplate template) throws IOException {
    return render(source, sourcePath, template, BrandingMetadata.empty());
  }

  public BufferedImage render(
      BufferedImage source,
      Path sourcePath,
      BrandingTemplate template,
      BrandingMetadata metadata
  ) throws IOException {
    metadata = metadata == null ? BrandingMetadata.empty() : metadata;
    // Step 1: trim the empty (no-participant) stretches from long strips, keeping the original pixels.
    BufferedImage image = autoCropper.crop(
        source,
        template.isAutoCropEnabled(),
        template.isCropBetweenParticipants(),
        template.isCropVerticalEnabled());
    // Step 2: apply an automatic, content-aware retouch so the photo looks better.
    image = imageEnhancer.enhanceIfEnabled(image, template.isEnhanceEnabled());
    // Step 3: build the poster (header, image, results, logos, text bar) around the resulting photo.
    PosterLayout layout = layout(image, template);
    BufferedImage output = new BufferedImage(layout.width(), layout.height(), BufferedImage.TYPE_INT_ARGB);
    Graphics2D graphics = output.createGraphics();
    try {
      applyQualityHints(graphics);
      graphics.setColor(ColorParser.parse(template.getCanvasBackgroundColor(), Color.WHITE));
      graphics.fillRect(0, 0, output.getWidth(), output.getHeight());
      drawSourceImage(graphics, image, layout.imageArea(), template);
      drawTextBar(graphics, sourcePath, layout.imageArea(), output.getWidth(), template, metadata);
      drawLogo(graphics, output, template);
      drawHeader(graphics, output, layout.headerHeight(), sourcePath, template, metadata);
      drawResultsTable(graphics, output, layout.resultsHeight(), sourcePath, template, metadata);
    } finally {
      graphics.dispose();
    }
    return output;
  }

  private static PosterLayout layout(BufferedImage image, BrandingTemplate template) {
    int imageWidth = image.getWidth();
    int imageHeight = image.getHeight();

    if (!template.isCanvasEnabled()) {
      return new PosterLayout(imageWidth, imageHeight, 0, 0, new Rectangle(0, 0, imageWidth, imageHeight));
    }

    if (template.getImageFitMode() == ImageFitMode.ORIGINAL) {
      // Keep the photo at its native resolution; the poster width follows the (cropped) photo width
      // and the header/results bands are sized relative to the photo height.
      int headerHeight = template.isHeaderEnabled()
          ? Math.max(0, (int) Math.round(imageHeight * template.getHeaderHeightPercent() / 100.0))
          : 0;
      int resultsHeight = template.isResultsEnabled()
          ? Math.max(0, (int) Math.round(imageHeight * template.getResultsHeightPercent() / 100.0))
          : 0;
      int height = headerHeight + imageHeight + resultsHeight;
      Rectangle imageArea = new Rectangle(0, headerHeight, imageWidth, imageHeight);
      return new PosterLayout(imageWidth, height, headerHeight, resultsHeight, imageArea);
    }

    int width = Math.max(320, template.getCanvasWidth());
    int height = Math.max(320, template.getCanvasHeight());
    int headerHeight = template.isHeaderEnabled()
        ? Math.max(0, (int) Math.round(height * template.getHeaderHeightPercent() / 100.0))
        : 0;
    int resultsHeight = template.isResultsEnabled()
        ? Math.max(0, (int) Math.round(height * template.getResultsHeightPercent() / 100.0))
        : 0;
    int areaHeight = Math.max(1, height - headerHeight - resultsHeight);
    Rectangle imageArea = new Rectangle(0, headerHeight, width, areaHeight);
    return new PosterLayout(width, height, headerHeight, resultsHeight, imageArea);
  }

  private void drawSourceImage(Graphics2D graphics, BufferedImage source, Rectangle target, BrandingTemplate template) {
    if (target.width <= 0 || target.height <= 0) {
      return;
    }

    if (template.getImageFitMode() == ImageFitMode.ORIGINAL) {
      // Draw at native 1:1 pixels; the image area was sized to match the photo, so nothing is scaled or cropped here.
      graphics.drawImage(source, target.x, target.y, null);
      return;
    }

    if (template.getImageFitMode() == null || template.getImageFitMode().name().equals("COVER")) {
      double scale = Math.max((double) target.width / source.getWidth(), (double) target.height / source.getHeight());
      int width = (int) Math.round(source.getWidth() * scale);
      int height = (int) Math.round(source.getHeight() * scale);
      int x = target.x + (target.width - width) / 2;
      int y = target.y + (target.height - height) / 2;
      graphics.drawImage(source, x, y, width, height, null);
      return;
    }

    if (template.getImageFitMode().name().equals("CONTAIN")) {
      double scale = Math.min((double) target.width / source.getWidth(), (double) target.height / source.getHeight());
      int width = (int) Math.round(source.getWidth() * scale);
      int height = (int) Math.round(source.getHeight() * scale);
      int x = target.x + (target.width - width) / 2;
      int y = target.y + (target.height - height) / 2;
      graphics.drawImage(source, x, y, width, height, null);
      return;
    }

    graphics.drawImage(source, target.x, target.y, target.width, target.height, null);
  }

  private void drawTextBar(
      Graphics2D graphics,
      Path sourcePath,
      Rectangle targetArea,
      int outputWidth,
      BrandingTemplate template,
      BrandingMetadata metadata
  ) {
    if (!template.isTextBarEnabled()) {
      return;
    }

    int barHeight = Math.max(24, (int) Math.round(targetArea.height * template.getTextBarHeightPercent() / 100.0));
    int y = targetArea.y + targetArea.height - barHeight;
    graphics.setColor(ColorParser.parse(template.getTextBarColor(), new Color(0, 0, 0, 190)));
    graphics.fillRect(targetArea.x, y, targetArea.width, barHeight);

    String text = textTemplateEngine.render(template.getTextTemplate(), sourcePath, metadata.placeholders());
    if (text.isBlank()) {
      return;
    }

    int horizontalPadding = Math.max(16, outputWidth / 80);
    int fontSize = Math.max(12, template.getFontSize());
    Font font = new Font(template.getFontName(), Font.BOLD, fontSize);
    FontMetrics metrics = graphics.getFontMetrics(font);
    while (metrics.stringWidth(text) > targetArea.width - horizontalPadding * 2 && fontSize > 12) {
      fontSize -= 2;
      font = font.deriveFont((float) fontSize);
      metrics = graphics.getFontMetrics(font);
    }

    int baseline = y + (barHeight - metrics.getHeight()) / 2 + metrics.getAscent();
    graphics.setFont(font);
    graphics.setColor(ColorParser.parse(template.getTextColor(), Color.WHITE));
    graphics.drawString(text, targetArea.x + horizontalPadding, baseline);
  }

  private void drawLogo(Graphics2D graphics, BufferedImage output, BrandingTemplate template) throws IOException {
    String logoPath = template.getLogoPath();
    if (logoPath == null || logoPath.isBlank()) {
      return;
    }

    Path path = Path.of(logoPath);
    if (!Files.isRegularFile(path)) {
      return;
    }

    BufferedImage logo = ImageIO.read(path.toFile());
    if (logo == null) {
      return;
    }

    int logoWidth = Math.max(1, (int) Math.round(output.getWidth() * template.getLogoScalePercent() / 100.0));
    int logoHeight = Math.max(1, (int) Math.round((double) logoWidth * logo.getHeight() / logo.getWidth()));
    int[] coordinates = logoCoordinates(output.getWidth(), output.getHeight(), logoWidth, logoHeight, template);

    float opacity = (float) Math.max(0.0, Math.min(1.0, template.getLogoOpacity()));
    Composite previousComposite = graphics.getComposite();
    graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
    graphics.drawImage(logo, coordinates[0], coordinates[1], logoWidth, logoHeight, null);
    graphics.setComposite(previousComposite);
  }

  private void drawHeader(
      Graphics2D graphics,
      BufferedImage output,
      int height,
      Path sourcePath,
      BrandingTemplate template,
      BrandingMetadata metadata
  ) throws IOException {
    if (!template.isHeaderEnabled() || height <= 0) {
      return;
    }

    // Paint the header background with the configured horizontal fade so the band is not a flat block.
    Color headerColor = ColorParser.parse(template.getHeaderBackgroundColor(), new Color(13, 91, 145));
    Color transparent = new Color(headerColor.getRed(), headerColor.getGreen(), headerColor.getBlue(), 0);
    HeaderFade fade = template.getHeaderFade() == null ? HeaderFade.LEFT_TO_RIGHT : template.getHeaderFade();
    switch (fade) {
      case NONE -> graphics.setPaint(headerColor);
      // Solid colour where the (left-aligned) text sits, fading out towards the right.
      case LEFT_TO_RIGHT -> graphics.setPaint(new GradientPaint(0, 0, headerColor, output.getWidth(), 0, transparent));
      case RIGHT_TO_LEFT -> graphics.setPaint(new GradientPaint(0, 0, transparent, output.getWidth(), 0, headerColor));
      default -> graphics.setPaint(headerColor);
    }
    graphics.fillRect(0, 0, output.getWidth(), height);

    // Horizontal padding follows the poster width; the vertical inset is bounded by the band height so
    // the header content always stays inside the band (matters for short native-size strips).
    int hPadding = Math.max(18, output.getWidth() / 40);
    int vPadding = Math.max(6, Math.min(hPadding, height / 6));
    int logoMaxHeight = Math.max(16, height - vPadding * 2);
    int leftLogoWidth = drawHeaderLogo(graphics, template.getHeaderLeftLogoPath(), hPadding, vPadding, output.getWidth() / 5, logoMaxHeight);
    int rightLogoWidth = drawHeaderLogoRight(
        graphics,
        template.getHeaderRightLogoPath(),
        output.getWidth() - hPadding,
        vPadding,
        output.getWidth() / 4,
        logoMaxHeight
    );

    int textX = hPadding + leftLogoWidth + (leftLogoWidth > 0 ? hPadding : 0);
    int textRight = output.getWidth() - hPadding - rightLogoWidth - (rightLogoWidth > 0 ? hPadding : 0);
    int textWidth = Math.max(80, textRight - textX);
    Color headerTextColor = ColorParser.parse(template.getHeaderTextColor(), Color.WHITE);

    String subtitle = (metadata.hasHeaderSubtitle()
        ? metadata.headerSubtitle()
        : textTemplateEngine.render(template.getHeaderSubtitle(), sourcePath, metadata.placeholders())).strip();
    int subtitleSize = subtitle.isBlank() ? 0 : Math.max(10, height / 9);
    int subtitleGap = subtitle.isBlank() ? 0 : Math.max(2, height / 20);

    // Reserve room for the subtitle, then size the title and limit the wrapped line count to what fits.
    int titleArea = Math.max(1, height - vPadding * 2 - subtitleSize - subtitleGap);
    int titleSize = Math.max(12, Math.min(height / 5, titleArea));
    int lineStep = titleSize + Math.max(2, titleSize / 7);
    int maxLines = Math.max(1, Math.min(3, titleArea / lineStep));

    Font titleFont = new Font(template.getFontName(), Font.BOLD, titleSize);
    int y = vPadding + titleSize;
    String title = metadata.hasHeaderTitle()
        ? metadata.headerTitle()
        : textTemplateEngine.render(template.getHeaderTitle(), sourcePath, metadata.placeholders());
    for (String line : wrapLines(title, graphics, titleFont, textWidth, maxLines)) {
      graphics.setFont(titleFont);
      drawTextWithShadow(graphics, line, textX, y, headerTextColor);
      y += lineStep;
    }

    if (!subtitle.isBlank()) {
      graphics.setFont(new Font(template.getFontName(), Font.PLAIN, subtitleSize));
      drawTextWithShadow(graphics, subtitle, textX, Math.min(height - vPadding, y + subtitleSize), headerTextColor);
    }
  }

  /** Draws text with a soft shadow so it stays legible over the faded part of the header background. */
  private static void drawTextWithShadow(Graphics2D graphics, String text, int x, int y, Color color) {
    int offset = Math.max(1, graphics.getFont().getSize() / 22);
    graphics.setColor(new Color(0, 0, 0, 120));
    graphics.drawString(text, x + offset, y + offset);
    graphics.setColor(color);
    graphics.drawString(text, x, y);
  }

  private int drawHeaderLogo(
      Graphics2D graphics,
      String logoPath,
      int x,
      int y,
      int maxWidth,
      int maxHeight
  ) throws IOException {
    BufferedImage logo = readOptionalImage(logoPath);
    if (logo == null) {
      return 0;
    }
    int[] size = containSize(logo.getWidth(), logo.getHeight(), maxWidth, maxHeight);
    graphics.drawImage(logo, x, y + (maxHeight - size[1]) / 2, size[0], size[1], null);
    return size[0];
  }

  private int drawHeaderLogoRight(
      Graphics2D graphics,
      String logoPath,
      int right,
      int y,
      int maxWidth,
      int maxHeight
  ) throws IOException {
    BufferedImage logo = readOptionalImage(logoPath);
    if (logo == null) {
      return 0;
    }
    int[] size = containSize(logo.getWidth(), logo.getHeight(), maxWidth, maxHeight);
    graphics.drawImage(logo, right - size[0], y + (maxHeight - size[1]) / 2, size[0], size[1], null);
    return size[0];
  }

  private void drawResultsTable(
      Graphics2D graphics,
      BufferedImage output,
      int height,
      Path sourcePath,
      BrandingTemplate template,
      BrandingMetadata metadata
  ) {
    if (!template.isResultsEnabled() || height <= 0) {
      return;
    }

    int y = output.getHeight() - height;
    Color background = ColorParser.parse(template.getResultsBackgroundColor(), new Color(32, 50, 65));
    Color header = ColorParser.parse(template.getResultsHeaderColor(), new Color(23, 38, 51));
    Color accent = ColorParser.parse(template.getResultsAccentColor(), new Color(240, 245, 0));

    graphics.setColor(background);
    graphics.fillRect(0, y, output.getWidth(), height);

    int padding = Math.max(12, output.getWidth() / 60);
    // Keep the title bar within the band even when it is short (native-size strips with a small results percent).
    int titleHeight = Math.min(Math.max(28, height / 7), Math.max(28, height * 3 / 5));
    graphics.setColor(header);
    graphics.fillRect(0, y, output.getWidth(), titleHeight);
    graphics.setColor(Color.WHITE);
    graphics.setFont(new Font(template.getFontName(), Font.BOLD, Math.max(16, titleHeight / 2)));
    String title = metadata.hasResultsTitle()
        ? metadata.resultsTitle()
        : textTemplateEngine.render(template.getResultsTitle(), sourcePath, metadata.placeholders());
    graphics.drawString(title, padding, y + titleHeight - Math.max(8, titleHeight / 5));

    int tableTop = y + titleHeight;
    int rowHeight = Math.max(22, (height - titleHeight) / 8);
    int headerRowHeight = Math.max(20, rowHeight);
    if (tableTop + headerRowHeight > output.getHeight()) {
      // No room below the title bar for the column header - leave the table empty rather than overflow the band.
      return;
    }
    graphics.setFont(new Font(template.getFontName(), Font.BOLD, Math.max(11, rowHeight / 3)));
    graphics.setColor(new Color(90, 170, 245));
    drawResultColumns(graphics, tableTop + headerRowHeight - 7, padding, output.getWidth(), new String[]{
        "PORADIE", "LN", "ATLET", "BIB", "VYSLEDOK", "ROZDIEL", "REAKCIA", "PB / SB"
    }, false, accent);

    String rowsText = metadata.hasResultsRowsText()
        ? metadata.resultsRowsText()
        : textTemplateEngine.render(template.getResultsRowsText(), sourcePath, metadata.placeholders());
    List<String[]> rows = parseResultRows(rowsText);
    graphics.setFont(new Font(template.getFontName(), Font.PLAIN, Math.max(12, rowHeight / 3 + 4)));
    int rowY = tableTop + headerRowHeight;
    for (int i = 0; i < rows.size() && rowY + rowHeight <= output.getHeight(); i++) {
      if (i % 2 == 0) {
        graphics.setColor(new Color(255, 255, 255, 16));
        graphics.fillRect(0, rowY, output.getWidth(), rowHeight);
      }
      graphics.setColor(Color.WHITE);
      drawResultColumns(graphics, rowY + rowHeight - 7, padding, output.getWidth(), rows.get(i), true, accent);
      rowY += rowHeight;
    }
  }

  private void drawResultColumns(
      Graphics2D graphics,
      int baseline,
      int padding,
      int width,
      String[] values,
      boolean resultAccent,
      Color accent
  ) {
    double[] starts = {0.02, 0.09, 0.20, 0.46, 0.56, 0.67, 0.77, 0.88};
    for (int i = 0; i < starts.length; i++) {
      String value = i < values.length ? values[i].trim() : "";
      if (resultAccent && i == 4) {
        graphics.setColor(accent);
        graphics.setFont(graphics.getFont().deriveFont(Font.BOLD));
      } else if (resultAccent && i == 2) {
        graphics.setColor(Color.WHITE);
        graphics.setFont(graphics.getFont().deriveFont(Font.BOLD));
      } else {
        graphics.setColor(resultAccent ? Color.WHITE : new Color(90, 170, 245));
        graphics.setFont(graphics.getFont().deriveFont(resultAccent ? Font.PLAIN : Font.BOLD));
      }
      graphics.drawString(value, padding + (int) Math.round(width * starts[i]), baseline);
    }
  }

  private static List<String[]> parseResultRows(String text) {
    return nullToEmpty(text).lines()
        .map(String::trim)
        .filter(line -> !line.isBlank())
        .map(line -> {
          if (line.contains("|")) {
            return line.split("\\|", -1);
          }
          if (line.contains(";")) {
            return line.split(";", -1);
          }
          return line.split(",", -1);
        })
        .toList();
  }

  private static int[] logoCoordinates(
      int imageWidth,
      int imageHeight,
      int logoWidth,
      int logoHeight,
      BrandingTemplate template
  ) {
    LogoPosition position = template.getLogoPosition();
    int offsetX = template.getOffsetX();
    int offsetY = template.getOffsetY();

    return switch (position) {
      case TOP_LEFT -> new int[]{offsetX, offsetY};
      case TOP_RIGHT -> new int[]{imageWidth - logoWidth - offsetX, offsetY};
      case BOTTOM_LEFT -> new int[]{offsetX, imageHeight - logoHeight - offsetY};
      case BOTTOM_RIGHT -> new int[]{imageWidth - logoWidth - offsetX, imageHeight - logoHeight - offsetY};
      case CENTER -> new int[]{(imageWidth - logoWidth) / 2 + offsetX, (imageHeight - logoHeight) / 2 + offsetY};
    };
  }

  private static BufferedImage readOptionalImage(String imagePath) throws IOException {
    if (imagePath == null || imagePath.isBlank()) {
      return null;
    }
    Path path = Path.of(imagePath);
    if (!Files.isRegularFile(path)) {
      return null;
    }
    return ImageIO.read(path.toFile());
  }

  private static int[] containSize(int sourceWidth, int sourceHeight, int maxWidth, int maxHeight) {
    double scale = Math.min((double) maxWidth / sourceWidth, (double) maxHeight / sourceHeight);
    return new int[]{
        Math.max(1, (int) Math.round(sourceWidth * scale)),
        Math.max(1, (int) Math.round(sourceHeight * scale))
    };
  }

  private static List<String> wrapLines(
      String text,
      Graphics2D graphics,
      Font font,
      int maxWidth,
      int maxLines
  ) {
    FontMetrics metrics = graphics.getFontMetrics(font);
    List<String> lines = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    for (String word : text.replace("\r", "").replace("\n", " ").split("\\s+")) {
      if (word.isBlank()) {
        continue;
      }
      String candidate = current.length() == 0 ? word : current + " " + word;
      if (metrics.stringWidth(candidate) <= maxWidth || current.length() == 0) {
        current = new StringBuilder(candidate);
      } else {
        lines.add(current.toString());
        current = new StringBuilder(word);
      }
      if (lines.size() == maxLines) {
        break;
      }
    }
    if (current.length() > 0 && lines.size() < maxLines) {
      lines.add(current.toString());
    }
    return lines;
  }

  private static String nullToEmpty(String value) {
    return value == null ? "" : value;
  }

  private static void applyQualityHints(Graphics2D graphics) {
    graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
  }
}
