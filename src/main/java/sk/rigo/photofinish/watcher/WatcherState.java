package sk.rigo.photofinish.watcher;

import java.nio.file.Path;

public record WatcherState(boolean running, Path directory, String message) {

  public static WatcherState stopped() {
    return new WatcherState(false, null, "Stopped");
  }

  public static WatcherState running(Path directory, String message) {
    return new WatcherState(true, directory, message);
  }

  public static WatcherState error(Path directory, String message) {
    return new WatcherState(false, directory, message);
  }
}
