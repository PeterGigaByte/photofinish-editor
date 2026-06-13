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
  private boolean canvasEnabled;
  private int canvasWidth;
  private int canvasHeight;
  private ImageFitMode imageFitMode;
  private boolean autoCropEnabled;
  private String canvasBackgroundColor;
  private boolean headerEnabled;
  private double headerHeightPercent;
  private String headerBackgroundColor;
  private String headerTextColor;
  private String headerTitle;
  private String headerSubtitle;
  private String headerLeftLogoPath;
  private String headerRightLogoPath;
  private boolean resultsEnabled;
  private double resultsHeightPercent;
  private String resultsTitle;
  private String resultsRowsText;
  private String resultsBackgroundColor;
  private String resultsHeaderColor;
  private String resultsAccentColor;

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
    template.setCanvasEnabled(true);
    template.setCanvasWidth(1080);
    template.setCanvasHeight(1920);
    template.setImageFitMode(ImageFitMode.ORIGINAL);
    template.setAutoCropEnabled(true);
    template.setCanvasBackgroundColor("#F6F8FA");
    template.setHeaderEnabled(true);
    template.setHeaderHeightPercent(10.0);
    template.setHeaderBackgroundColor("#0D5B91");
    template.setHeaderTextColor("#FFFFFF");
    template.setHeaderTitle("EVENT TITLE");
    template.setHeaderSubtitle("Venue  |  Date");
    template.setHeaderLeftLogoPath("");
    template.setHeaderRightLogoPath("");
    template.setResultsEnabled(true);
    template.setResultsHeightPercent(24.0);
    template.setResultsTitle("Race results");
    template.setResultsRowsText("""
        1|4|ATHLETE One|1|6.86||0.154|PB
        2|5|ATHLETE Two|2|7.06|+0.20|0.154|
        3|6|ATHLETE Three|7|7.07|+0.21|0.148|SB
        """);
    template.setResultsBackgroundColor("#203241");
    template.setResultsHeaderColor("#172633");
    template.setResultsAccentColor("#F0F500");
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

  public boolean isCanvasEnabled() {
    return canvasEnabled;
  }

  public void setCanvasEnabled(boolean canvasEnabled) {
    this.canvasEnabled = canvasEnabled;
  }

  public int getCanvasWidth() {
    return canvasWidth;
  }

  public void setCanvasWidth(int canvasWidth) {
    this.canvasWidth = canvasWidth;
  }

  public int getCanvasHeight() {
    return canvasHeight;
  }

  public void setCanvasHeight(int canvasHeight) {
    this.canvasHeight = canvasHeight;
  }

  public ImageFitMode getImageFitMode() {
    return imageFitMode;
  }

  public void setImageFitMode(ImageFitMode imageFitMode) {
    this.imageFitMode = imageFitMode;
  }

  public boolean isAutoCropEnabled() {
    return autoCropEnabled;
  }

  public void setAutoCropEnabled(boolean autoCropEnabled) {
    this.autoCropEnabled = autoCropEnabled;
  }

  public String getCanvasBackgroundColor() {
    return canvasBackgroundColor;
  }

  public void setCanvasBackgroundColor(String canvasBackgroundColor) {
    this.canvasBackgroundColor = canvasBackgroundColor;
  }

  public boolean isHeaderEnabled() {
    return headerEnabled;
  }

  public void setHeaderEnabled(boolean headerEnabled) {
    this.headerEnabled = headerEnabled;
  }

  public double getHeaderHeightPercent() {
    return headerHeightPercent;
  }

  public void setHeaderHeightPercent(double headerHeightPercent) {
    this.headerHeightPercent = headerHeightPercent;
  }

  public String getHeaderBackgroundColor() {
    return headerBackgroundColor;
  }

  public void setHeaderBackgroundColor(String headerBackgroundColor) {
    this.headerBackgroundColor = headerBackgroundColor;
  }

  public String getHeaderTextColor() {
    return headerTextColor;
  }

  public void setHeaderTextColor(String headerTextColor) {
    this.headerTextColor = headerTextColor;
  }

  public String getHeaderTitle() {
    return headerTitle;
  }

  public void setHeaderTitle(String headerTitle) {
    this.headerTitle = headerTitle;
  }

  public String getHeaderSubtitle() {
    return headerSubtitle;
  }

  public void setHeaderSubtitle(String headerSubtitle) {
    this.headerSubtitle = headerSubtitle;
  }

  public String getHeaderLeftLogoPath() {
    return headerLeftLogoPath;
  }

  public void setHeaderLeftLogoPath(String headerLeftLogoPath) {
    this.headerLeftLogoPath = headerLeftLogoPath;
  }

  public String getHeaderRightLogoPath() {
    return headerRightLogoPath;
  }

  public void setHeaderRightLogoPath(String headerRightLogoPath) {
    this.headerRightLogoPath = headerRightLogoPath;
  }

  public boolean isResultsEnabled() {
    return resultsEnabled;
  }

  public void setResultsEnabled(boolean resultsEnabled) {
    this.resultsEnabled = resultsEnabled;
  }

  public double getResultsHeightPercent() {
    return resultsHeightPercent;
  }

  public void setResultsHeightPercent(double resultsHeightPercent) {
    this.resultsHeightPercent = resultsHeightPercent;
  }

  public String getResultsTitle() {
    return resultsTitle;
  }

  public void setResultsTitle(String resultsTitle) {
    this.resultsTitle = resultsTitle;
  }

  public String getResultsRowsText() {
    return resultsRowsText;
  }

  public void setResultsRowsText(String resultsRowsText) {
    this.resultsRowsText = resultsRowsText;
  }

  public String getResultsBackgroundColor() {
    return resultsBackgroundColor;
  }

  public void setResultsBackgroundColor(String resultsBackgroundColor) {
    this.resultsBackgroundColor = resultsBackgroundColor;
  }

  public String getResultsHeaderColor() {
    return resultsHeaderColor;
  }

  public void setResultsHeaderColor(String resultsHeaderColor) {
    this.resultsHeaderColor = resultsHeaderColor;
  }

  public String getResultsAccentColor() {
    return resultsAccentColor;
  }

  public void setResultsAccentColor(String resultsAccentColor) {
    this.resultsAccentColor = resultsAccentColor;
  }
}
