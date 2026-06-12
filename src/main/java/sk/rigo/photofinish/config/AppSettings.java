package sk.rigo.photofinish.config;

public class AppSettings {

  private String inputDirectory;
  private String exportDirectory;
  private long activeTemplateId;
  private String latestJsonUrl;
  private boolean autoStartWatcher;

  public String getInputDirectory() {
    return inputDirectory;
  }

  public void setInputDirectory(String inputDirectory) {
    this.inputDirectory = inputDirectory;
  }

  public String getExportDirectory() {
    return exportDirectory;
  }

  public void setExportDirectory(String exportDirectory) {
    this.exportDirectory = exportDirectory;
  }

  public long getActiveTemplateId() {
    return activeTemplateId;
  }

  public void setActiveTemplateId(long activeTemplateId) {
    this.activeTemplateId = activeTemplateId;
  }

  public String getLatestJsonUrl() {
    return latestJsonUrl;
  }

  public void setLatestJsonUrl(String latestJsonUrl) {
    this.latestJsonUrl = latestJsonUrl;
  }

  public boolean isAutoStartWatcher() {
    return autoStartWatcher;
  }

  public void setAutoStartWatcher(boolean autoStartWatcher) {
    this.autoStartWatcher = autoStartWatcher;
  }
}
