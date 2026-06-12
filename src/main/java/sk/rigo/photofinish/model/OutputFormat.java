package sk.rigo.photofinish.model;

public enum OutputFormat {
  JPG("jpg", "JPEG"),
  PNG("png", "PNG");

  private final String extension;
  private final String imageIoFormat;

  OutputFormat(String extension, String imageIoFormat) {
    this.extension = extension;
    this.imageIoFormat = imageIoFormat;
  }

  public String extension() {
    return extension;
  }

  public String imageIoFormat() {
    return imageIoFormat;
  }
}
