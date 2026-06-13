package sk.rigo.photofinish.image;

import sk.rigo.photofinish.model.BrandingTemplate;
import sk.rigo.photofinish.model.LogoPosition;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
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

  public BufferedImage render(Path sourcePath, BrandingTemplate template) throws IOException {
    BufferedImage source = ImageIO.read(sourcePath.toFile());
    if (source == null) {
      throw new IOException("Unsupported image file: " + sourcePath);
    }

    return render(source, sourcePath, template);
  }

  public BufferedImage render(BufferedImage source, Path sourcePath, BrandingTemplate template) throws IOException {
    int outputWidth = template.isCanvasEnabled() ? Math.max(320, template.getCanvasWidth()) : source.getWidth();
    int outputHeight = template.isCanvasEnabled() ? Math.max(320, template.getCanvasHeight()) : source.getHeight();
    BufferedImage output = new BufferedImage(outputWidth, outputHeight, BufferedImage.TYPE_INT_ARGB);
    Graphics2D graphics = output.createGraphics();
    try {
      applyQualityHints(graphics);
      graphics.setColor(ColorParser.parse(template.getCanvasBackgroundColor(), Color.WHITE));
      graphics.fillRect(0, 0, output.getWidth(), output.getHeight());
      Rectangle imageArea = imageArea(output, template);
      drawSourceImage(graphics, source, imageArea, template);
      drawTextBar(graphics, sourcePath, output, template);
      drawLogo(graphics, output, template);
      drawHeader(graphics, output, template);
      drawResultsTable(graphics, output, template);
    } finally {
      graphics.dispose();
    }
    return output;
  }

  private void drawSourceImage(Graphics2D graphics, BufferedImage source, Rectangle target, BrandingTemplate template) {
    if (target.width <= 0 || target.height <= 0) {
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

  private void drawTextBar(Graphics2D graphics, Path sourcePath, BufferedImage output, BrandingTemplate template) {
    if (!template.isTextBarEnabled()) {
      return;
    }

    Rectangle targetArea = imageArea(output, template);
    int barHeight = Math.max(24, (int) Math.round(targetArea.height * template.getTextBarHeightPercent() / 100.0));
    int y = targetArea.y + targetArea.height - barHeight;
    graphics.setColor(ColorParser.parse(template.getTextBarColor(), new Color(0, 0, 0, 190)));
    graphics.fillRect(targetArea.x, y, targetArea.width, barHeight);

    String text = textTemplateEngine.render(template.getTextTemplate(), sourcePath);
    if (text.isBlank()) {
      return;
    }

    int horizontalPadding = Math.max(16, output.getWidth() / 80);
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

  private void drawHeader(Graphics2D graphics, BufferedImage output, BrandingTemplate template) throws IOException {
    if (!template.isHeaderEnabled()) {
      return;
    }

    int height = headerHeight(output, template);
    if (height <= 0) {
      return;
    }

    graphics.setColor(ColorParser.parse(template.getHeaderBackgroundColor(), new Color(13, 91, 145)));
    graphics.fillRect(0, 0, output.getWidth(), height);

    int padding = Math.max(18, output.getWidth() / 40);
    int logoMaxHeight = Math.max(24, height - padding * 2);
    int leftLogoWidth = drawHeaderLogo(graphics, template.getHeaderLeftLogoPath(), padding, padding, output.getWidth() / 5, logoMaxHeight);
    int rightLogoWidth = drawHeaderLogoRight(
        graphics,
        template.getHeaderRightLogoPath(),
        output.getWidth() - padding,
        padding,
        output.getWidth() / 4,
        logoMaxHeight
    );

    int textX = padding + leftLogoWidth + (leftLogoWidth > 0 ? padding : 0);
    int textRight = output.getWidth() - padding - rightLogoWidth - (rightLogoWidth > 0 ? padding : 0);
    int textWidth = Math.max(80, textRight - textX);
    graphics.setColor(ColorParser.parse(template.getHeaderTextColor(), Color.WHITE));

    int titleSize = Math.max(18, height / 5);
    Font titleFont = new Font(template.getFontName(), Font.BOLD, titleSize);
    int y = padding + titleSize;
    for (String line : wrapLines(nullToEmpty(template.getHeaderTitle()), graphics, titleFont, textWidth, 3)) {
      graphics.setFont(titleFont);
      graphics.drawString(line, textX, y);
      y += titleSize + Math.max(2, titleSize / 7);
    }

    String subtitle = nullToEmpty(template.getHeaderSubtitle());
    if (!subtitle.isBlank()) {
      int subtitleSize = Math.max(12, height / 9);
      graphics.setFont(new Font(template.getFontName(), Font.PLAIN, subtitleSize));
      graphics.drawString(subtitle, textX, Math.min(height - padding / 2, y + subtitleSize));
    }
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

  private void drawResultsTable(Graphics2D graphics, BufferedImage output, BrandingTemplate template) {
    if (!template.isResultsEnabled()) {
      return;
    }

    int height = resultsHeight(output, template);
    if (height <= 0) {
      return;
    }

    int y = output.getHeight() - height;
    Color background = ColorParser.parse(template.getResultsBackgroundColor(), new Color(32, 50, 65));
    Color header = ColorParser.parse(template.getResultsHeaderColor(), new Color(23, 38, 51));
    Color accent = ColorParser.parse(template.getResultsAccentColor(), new Color(240, 245, 0));

    graphics.setColor(background);
    graphics.fillRect(0, y, output.getWidth(), height);

    int padding = Math.max(12, output.getWidth() / 60);
    int titleHeight = Math.max(34, height / 7);
    graphics.setColor(header);
    graphics.fillRect(0, y, output.getWidth(), titleHeight);
    graphics.setColor(Color.WHITE);
    graphics.setFont(new Font(template.getFontName(), Font.BOLD, Math.max(18, titleHeight / 2)));
    graphics.drawString(nullToEmpty(template.getResultsTitle()), padding, y + titleHeight - Math.max(8, titleHeight / 5));

    int tableTop = y + titleHeight;
    int rowHeight = Math.max(22, (height - titleHeight) / 8);
    int headerRowHeight = Math.max(20, rowHeight);
    graphics.setFont(new Font(template.getFontName(), Font.BOLD, Math.max(11, rowHeight / 3)));
    graphics.setColor(new Color(90, 170, 245));
    drawResultColumns(graphics, tableTop + headerRowHeight - 7, padding, output.getWidth(), new String[]{
        "PORADIE", "LN", "ATLET", "BIB", "VYSLEDOK", "ROZDIEL", "REAKCIA", "PB / SB"
    }, false, accent);

    List<String[]> rows = parseResultRows(template.getResultsRowsText());
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

  private static Rectangle imageArea(BufferedImage output, BrandingTemplate template) {
    int headerHeight = headerHeight(output, template);
    int resultsHeight = resultsHeight(output, template);
    int imageHeight = Math.max(1, output.getHeight() - headerHeight - resultsHeight);
    return new Rectangle(0, headerHeight, output.getWidth(), imageHeight);
  }

  private static int headerHeight(BufferedImage output, BrandingTemplate template) {
    if (!template.isCanvasEnabled() || !template.isHeaderEnabled()) {
      return 0;
    }
    return Math.max(0, (int) Math.round(output.getHeight() * template.getHeaderHeightPercent() / 100.0));
  }

  private static int resultsHeight(BufferedImage output, BrandingTemplate template) {
    if (!template.isCanvasEnabled() || !template.isResultsEnabled()) {
      return 0;
    }
    return Math.max(0, (int) Math.round(output.getHeight() * template.getResultsHeightPercent() / 100.0));
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
