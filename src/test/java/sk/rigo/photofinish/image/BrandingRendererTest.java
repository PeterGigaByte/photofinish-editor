package sk.rigo.photofinish.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import sk.rigo.photofinish.model.BrandingTemplate;
import sk.rigo.photofinish.model.HeaderFade;
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
    template.setAutoCropEnabled(false); // isolate the layout from cropping for a deterministic size
    template.setCropVerticalEnabled(false);
    template.setEnhanceEnabled(false);
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
    template.setCropVerticalEnabled(false);
    template.setEnhanceEnabled(false);
    template.setHeaderEnabled(true);
    template.setHeaderHeightPercent(5.0); // very short header band
    template.setResultsEnabled(true);
    template.setResultsHeightPercent(10.0);

    BufferedImage rendered = renderer.render(solid(1600, 120), Path.of("race2.jpg"), template);

    assertEquals(1600, rendered.getWidth());
    assertTrue(rendered.getHeight() > 120, "header and results bands should add height");
  }

  @Test
  void headerBackgroundFadesRightToLeft() throws Exception {
    BrandingTemplate template = BrandingTemplate.defaults();
    template.setImageFitMode(ImageFitMode.ORIGINAL);
    template.setAutoCropEnabled(false);
    template.setHeaderEnabled(true);
    template.setHeaderHeightPercent(30.0);
    template.setHeaderFade(HeaderFade.RIGHT_TO_LEFT);
    template.setHeaderBackgroundColor("#0D5B91");
    template.setHeaderTitle(""); // keep the sampled band free of text
    template.setHeaderSubtitle("");
    template.setHeaderLeftLogoPath("");
    template.setHeaderRightLogoPath("");
    template.setResultsEnabled(false);
    template.setTextBarEnabled(false);
    template.setLogoPath("");

    BufferedImage rendered = renderer.render(solid(1200, 200), Path.of("race.jpg"), template);

    Color headerColor = new Color(0x0D, 0x5B, 0x91);
    int y = 4;
    Color left = new Color(rendered.getRGB(4, y));
    Color right = new Color(rendered.getRGB(rendered.getWidth() - 4, y));

    // Right edge is essentially the full header colour; left edge has faded away from it.
    assertTrue(distance(right, headerColor) < 30, "right edge should be the header colour");
    assertTrue(distance(left, headerColor) > 100, "left edge should have faded away from the header colour");
  }

  private static double distance(Color a, Color b) {
    int dr = a.getRed() - b.getRed();
    int dg = a.getGreen() - b.getGreen();
    int db = a.getBlue() - b.getBlue();
    return Math.sqrt((double) dr * dr + dg * dg + db * db);
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
