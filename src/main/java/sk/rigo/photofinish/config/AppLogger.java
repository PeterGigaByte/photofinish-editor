package sk.rigo.photofinish.config;

import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public final class AppLogger {

  private AppLogger() {
  }

  public static void configure(Path logsDirectory) throws IOException {
    Logger rootLogger = Logger.getLogger("");
    FileHandler fileHandler = new FileHandler(
        logsDirectory.resolve("photofinish-%g.log").toString(),
        1_048_576,
        5,
        true
    );
    fileHandler.setFormatter(new SimpleFormatter());
    fileHandler.setLevel(Level.INFO);
    rootLogger.addHandler(fileHandler);
    rootLogger.setLevel(Level.INFO);
  }
}
