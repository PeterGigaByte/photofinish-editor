package sk.rigo.photofinish.model;

public enum ImageFitMode {
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
