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
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class BrandingRenderer {

  private final TextTemplateEngine textTemplateEngine = new TextTemplateEngine();

  public BufferedImage render(Path sourcePath, BrandingTemplate template) throws IOException {
    BufferedImage source = ImageIO.read(sourcePath.toFile());
    if (source == null) {
      throw new IOException("Unsupported image file: " + sourcePath);
    }

    BufferedImage output = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
    Graphics2D graphics = output.createGraphics();
    try {
      applyQualityHints(graphics);
      graphics.drawImage(source, 0, 0, null);
      drawTextBar(graphics, sourcePath, output, template);
      drawLogo(graphics, output, template);
    } finally {
      graphics.dispose();
    }
    return output;
  }

  private void drawTextBar(Graphics2D graphics, Path sourcePath, BufferedImage output, BrandingTemplate template) {
    if (!template.isTextBarEnabled()) {
      return;
    }

    int barHeight = Math.max(24, (int) Math.round(output.getHeight() * template.getTextBarHeightPercent() / 100.0));
    int y = output.getHeight() - barHeight;
    graphics.setColor(ColorParser.parse(template.getTextBarColor(), new Color(0, 0, 0, 190)));
    graphics.fillRect(0, y, output.getWidth(), barHeight);

    String text = textTemplateEngine.render(template.getTextTemplate(), sourcePath);
    if (text.isBlank()) {
      return;
    }

    int horizontalPadding = Math.max(16, output.getWidth() / 80);
    int fontSize = Math.max(12, template.getFontSize());
    Font font = new Font(template.getFontName(), Font.BOLD, fontSize);
    FontMetrics metrics = graphics.getFontMetrics(font);
    while (metrics.stringWidth(text) > output.getWidth() - horizontalPadding * 2 && fontSize > 12) {
      fontSize -= 2;
      font = font.deriveFont((float) fontSize);
      metrics = graphics.getFontMetrics(font);
    }

    int baseline = y + (barHeight - metrics.getHeight()) / 2 + metrics.getAscent();
    graphics.setFont(font);
    graphics.setColor(ColorParser.parse(template.getTextColor(), Color.WHITE));
    graphics.drawString(text, horizontalPadding, baseline);
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

  private static void applyQualityHints(Graphics2D graphics) {
    graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
  }
}
