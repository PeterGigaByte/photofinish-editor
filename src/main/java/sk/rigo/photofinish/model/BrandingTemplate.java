package sk.rigo.photofinish.model;

public class BrandingTemplate {

  private long id;
  private String name;
  private String logoPath;
  private LogoPosition logoPosition;
  private double logoScalePercent;
  private double logoOpacity;
  private int offsetX;
  private int offsetY;
  private boolean textBarEnabled;
  private String textTemplate;
  private double textBarHeightPercent;
  private String textBarColor;
  private String textColor;
  private String fontName;
  private int fontSize;
  private OutputFormat outputFormat;

  public static BrandingTemplate defaults() {
    BrandingTemplate template = new BrandingTemplate();
    template.setName("Default branding");
    template.setLogoPath("");
    template.setLogoPosition(LogoPosition.BOTTOM_RIGHT);
    template.setLogoScalePercent(18.0);
    template.setLogoOpacity(0.85);
    template.setOffsetX(24);
    template.setOffsetY(24);
    template.setTextBarEnabled(true);
    template.setTextTemplate("{filename}  |  {date} {time}");
    template.setTextBarHeightPercent(8.0);
    template.setTextBarColor("#CC111111");
    template.setTextColor("#FFFFFF");
    template.setFontName("Arial");
    template.setFontSize(32);
    template.setOutputFormat(OutputFormat.JPG);
    return template;
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getLogoPath() {
    return logoPath;
  }

  public void setLogoPath(String logoPath) {
    this.logoPath = logoPath;
  }

  public LogoPosition getLogoPosition() {
    return logoPosition;
  }

  public void setLogoPosition(LogoPosition logoPosition) {
    this.logoPosition = logoPosition;
  }

  public double getLogoScalePercent() {
    return logoScalePercent;
  }

  public void setLogoScalePercent(double logoScalePercent) {
    this.logoScalePercent = logoScalePercent;
  }

  public double getLogoOpacity() {
    return logoOpacity;
  }

  public void setLogoOpacity(double logoOpacity) {
    this.logoOpacity = logoOpacity;
  }

  public int getOffsetX() {
    return offsetX;
  }

  public void setOffsetX(int offsetX) {
    this.offsetX = offsetX;
  }

  public int getOffsetY() {
    return offsetY;
  }

  public void setOffsetY(int offsetY) {
    this.offsetY = offsetY;
  }

  public boolean isTextBarEnabled() {
    return textBarEnabled;
  }

  public void setTextBarEnabled(boolean textBarEnabled) {
    this.textBarEnabled = textBarEnabled;
  }

  public String getTextTemplate() {
    return textTemplate;
  }

  public void setTextTemplate(String textTemplate) {
    this.textTemplate = textTemplate;
  }

  public double getTextBarHeightPercent() {
    return textBarHeightPercent;
  }

  public void setTextBarHeightPercent(double textBarHeightPercent) {
    this.textBarHeightPercent = textBarHeightPercent;
  }

  public String getTextBarColor() {
    return textBarColor;
  }

  public void setTextBarColor(String textBarColor) {
    this.textBarColor = textBarColor;
  }

  public String getTextColor() {
    return textColor;
  }

  public void setTextColor(String textColor) {
    this.textColor = textColor;
  }

  public String getFontName() {
    return fontName;
  }

  public void setFontName(String fontName) {
    this.fontName = fontName;
  }

  public int getFontSize() {
    return fontSize;
  }

  public void setFontSize(int fontSize) {
    this.fontSize = fontSize;
  }

  public OutputFormat getOutputFormat() {
    return outputFormat;
  }

  public void setOutputFormat(OutputFormat outputFormat) {
    this.outputFormat = outputFormat;
  }
}
