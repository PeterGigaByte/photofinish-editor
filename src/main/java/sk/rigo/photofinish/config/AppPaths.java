package sk.rigo.photofinish.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public record AppPaths(Path dataDirectory, Path logsDirectory, Path stagingDirectory, Path databaseFile) {

  private static final String APP_DIR_NAME = "PhotoFinish Branding Studio";

  public static AppPaths create() throws IOException {
    Path dataDirectory = resolveDataDirectory();
    Path logsDirectory = dataDirectory.resolve("logs");
    Path stagingDirectory = dataDirectory.resolve("staging");

    Files.createDirectories(dataDirectory);
    Files.createDirectories(logsDirectory);
    Files.createDirectories(stagingDirectory);

    return new AppPaths(
        dataDirectory,
        logsDirectory,
        stagingDirectory,
        dataDirectory.resolve("photofinish.db")
    );
  }

  public Path defaultInputDirectory() {
    return dataDirectory.resolve("input");
  }

  public Path defaultExportDirectory() {
    return dataDirectory.resolve("export");
  }

  private static Path resolveDataDirectory() {
    String appData = System.getenv("APPDATA");
    if (appData != null && !appData.isBlank()) {
      return Path.of(appData).resolve(APP_DIR_NAME);
    }
    return Path.of(System.getProperty("user.home"), ".photofinish-branding-studio");
  }
}
