package sk.rigo.photofinish.model;

/** Direction of the header background fade. */
public enum HeaderFade {
  NONE("Solid (no fade)"),
  LEFT_TO_RIGHT("Fade out to the right"),
  RIGHT_TO_LEFT("Fade out to the left");

  private final String label;

  HeaderFade(String label) {
    this.label = label;
  }

  @Override
  public String toString() {
    return label;
  }
}
