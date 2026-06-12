package sk.rigo.photofinish.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public record AppMetadata(String name, String version, String vendor) {

  public static AppMetadata load() {
    Properties properties = new Properties();
    try (InputStream inputStream = AppMetadata.class.getResourceAsStream("/application.properties")) {
      if (inputStream != null) {
        properties.load(inputStream);
      }
    } catch (IOException ignored) {
      // Defaults below keep the app bootable even if resources are missing in a custom package.
    }
    return new AppMetadata(
        properties.getProperty("app.name", "PhotoFinish Branding Studio"),
        properties.getProperty("app.version", "0.1.0"),
        properties.getProperty("app.vendor", "Rigo")
    );
  }
}
