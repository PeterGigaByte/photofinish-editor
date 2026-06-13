package sk.rigo.photofinish.model;

public enum ImageFitMode {
  ORIGINAL("Keep original size"),
  COVER("Cover"),
  CONTAIN("Contain"),
  STRETCH("Stretch");

  private final String label;

  ImageFitMode(String label) {
    this.label = label;
  }

  @Override
  public String toString() {
    return label;
  }
}
