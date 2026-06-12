package sk.rigo.photofinish.image;

import java.awt.Color;

public final class ColorParser {

  private ColorParser() {
  }

  public static Color parse(String value, Color fallback) {
    if (value == null || value.isBlank()) {
      return fallback;
    }

    String normalized = value.trim();
    if (normalized.startsWith("#")) {
      normalized = normalized.substring(1);
    }

    try {
      if (normalized.length() == 6) {
        int rgb = Integer.parseUnsignedInt(normalized, 16);
        return new Color(rgb);
      }
      if (normalized.length() == 8) {
        int argb = (int) Long.parseLong(normalized, 16);
        int alpha = (argb >> 24) & 0xFF;
        int red = (argb >> 16) & 0xFF;
        int green = (argb >> 8) & 0xFF;
        int blue = argb & 0xFF;
        return new Color(red, green, blue, alpha);
      }
    } catch (NumberFormatException ignored) {
      return fallback;
    }
    return fallback;
  }
}
