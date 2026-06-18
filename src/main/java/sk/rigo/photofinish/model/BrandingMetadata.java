package sk.rigo.photofinish.model;

import java.util.Map;

public record BrandingMetadata(
    Map<String, String> placeholders,
    String headerTitle,
    String headerSubtitle,
    String resultsTitle,
    String resultsRowsText
) {

  public BrandingMetadata {
    placeholders = placeholders == null ? Map.of() : Map.copyOf(placeholders);
    headerTitle = nullToEmpty(headerTitle);
    headerSubtitle = nullToEmpty(headerSubtitle);
    resultsTitle = nullToEmpty(resultsTitle);
    resultsRowsText = nullToEmpty(resultsRowsText);
  }

  public static BrandingMetadata empty() {
    return new BrandingMetadata(Map.of(), "", "", "", "");
  }

  public boolean hasHeaderTitle() {
    return !headerTitle.isBlank();
  }

  public boolean hasHeaderSubtitle() {
    return !headerSubtitle.isBlank();
  }

  public boolean hasResultsTitle() {
    return !resultsTitle.isBlank();
  }

  public boolean hasResultsRowsText() {
    return !resultsRowsText.isBlank();
  }

  private static String nullToEmpty(String value) {
    return value == null ? "" : value;
  }
}
