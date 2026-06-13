package sk.rigo.photofinish.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import sk.rigo.photofinish.model.BrandingTemplate;
import sk.rigo.photofinish.model.ImageFitMode;
import org.junit.jupiter.api.Test;

class BrandingRendererTest {

  private final BrandingRenderer renderer = new BrandingRenderer();

  @Test
  void originalModeKeepsPhotoWidthAndAddsBands() throws Exception {
    int width = 1200;
    int height = 200;
    BrandingTemplate template = BrandingTemplate.defaults();
    template.setImageFitMode(ImageFitMode.ORIGINAL);
    template.setAutoCropEnabled(false); // isolate the layout from auto-crop for a deterministic width
    template.setHeaderEnabled(true);
    template.setHeaderHeightPercent(10.0);
    template.setResultsEnabled(true);
    template.setResultsHeightPercent(24.0);
    template.setCanvasWidth(1080); // narrower than the photo on purpose

    BufferedImage rendered = renderer.render(solid(width, height), Path.of("race1.jpg"), template);

    // Width follows the photo (native size), not the configured canvas width.
    assertEquals(width, rendered.getWidth());
    // Height grows by the header and results bands sized from the photo height.
    int headerHeight = (int) Math.round(height * 0.10);
    int resultsHeight = (int) Math.round(height * 0.24);
    assertEquals(headerHeight + height + resultsHeight, rendered.getHeight());
  }

  @Test
  void originalModeHandlesShortBandsWithoutError() throws Exception {
    BrandingTemplate template = BrandingTemplate.defaults();
    template.setImageFitMode(ImageFitMode.ORIGINAL);
    template.setAutoCropEnabled(false);
    template.setHeaderEnabled(true);
    template.setHeaderHeightPercent(5.0); // very short header band
    template.setResultsEnabled(true);
    template.setResultsHeightPercent(10.0);

    BufferedImage rendered = renderer.render(solid(1600, 120), Path.of("race2.jpg"), template);

    assertEquals(1600, rendered.getWidth());
    assertTrue(rendered.getHeight() > 120, "header and results bands should add height");
  }

  private static BufferedImage solid(int width, int height) {
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = image.createGraphics();
    graphics.setColor(new Color(60, 120, 200));
    graphics.fillRect(0, 0, width, height);
    graphics.dispose();
    return image;
  }
}
