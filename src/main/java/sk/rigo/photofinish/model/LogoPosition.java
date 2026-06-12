package sk.rigo.photofinish.model;

public enum LogoPosition {
  TOP_LEFT("Top left"),
  TOP_RIGHT("Top right"),
  BOTTOM_LEFT("Bottom left"),
  BOTTOM_RIGHT("Bottom right"),
  CENTER("Center");

  private final String label;

  LogoPosition(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }

  @Override
  public String toString() {
    return label;
  }
}
