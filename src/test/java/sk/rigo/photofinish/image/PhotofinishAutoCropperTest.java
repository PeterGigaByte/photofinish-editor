package sk.rigo.photofinish.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;

class PhotofinishAutoCropperTest {

  private final PhotofinishAutoCropper cropper = new PhotofinishAutoCropper();

  @Test
  void trimsEmptyEndsKeepsParticipantSpanAndOriginalHeight() {
    int width = 1000;
    int height = 200;
    int participantStart = 400;
    int participantEnd = 600;
    BufferedImage strip = stripWithParticipant(width, height, participantStart, participantEnd);

    BufferedImage cropped = cropper.cropIfBeneficial(strip, true);

    // Empty leading/trailing track is removed, so the result is narrower than the source...
    assertTrue(cropped.getWidth() < width, "expected empty ends to be cropped");
    // ...but still wide enough to contain the whole participant span...
    assertTrue(
        cropped.getWidth() >= participantEnd - participantStart,
        "participant span must be preserved");
    // ...and the kept pixels are at original scale (height unchanged, nothing resized).
    assertEquals(height, cropped.getHeight());
  }

  @Test
  void leavesShortImagesUntouched() {
    BufferedImage square = stripWithParticipant(200, 200, 80, 120);
    assertSame(square, cropper.cropIfBeneficial(square, true), "non-long images must not be cropped");
  }

  @Test
  void leavesBlankStripUntouched() {
    BufferedImage blank = new BufferedImage(1000, 200, BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = blank.createGraphics();
    graphics.setColor(new Color(128, 128, 128));
    graphics.fillRect(0, 0, 1000, 200);
    graphics.dispose();
    assertSame(blank, cropper.cropIfBeneficial(blank, true), "a strip with no participant must be left intact");
  }

  @Test
  void respectsDisabledFlag() {
    BufferedImage strip = stripWithParticipant(1000, 200, 400, 600);
    assertSame(strip, cropper.cropIfBeneficial(strip, false), "cropping must be skipped when disabled");
  }

  private static BufferedImage stripWithParticipant(int width, int height, int start, int end) {
    BufferedImage strip = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = strip.createGraphics();
    // Uniform background = empty track.
    graphics.setColor(new Color(128, 128, 128));
    graphics.fillRect(0, 0, width, height);
    // A high-contrast block standing in for an athlete crossing the line.
    graphics.setColor(new Color(20, 20, 20));
    graphics.fillRect(start, 0, end - start, height);
    graphics.dispose();
    return strip;
  }
}
