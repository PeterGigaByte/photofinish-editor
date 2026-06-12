package sk.rigo.photofinish.watcher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

public class StableFileDetector {

  private static final int REQUIRED_STABLE_READS = 3;

  public void waitUntilStable(Path path, Duration timeout, Duration interval) throws IOException, InterruptedException {
    long deadline = System.nanoTime() + timeout.toNanos();
    long previousSize = -1L;
    long previousModified = -1L;
    int stableReads = 0;

    while (System.nanoTime() < deadline) {
      if (!Files.isRegularFile(path)) {
        stableReads = 0;
        sleep(interval);
        continue;
      }

      long size = Files.size(path);
      long modified = Files.getLastModifiedTime(path).toMillis();
      if (size > 0L && size == previousSize && modified == previousModified) {
        stableReads++;
        if (stableReads >= REQUIRED_STABLE_READS) {
          return;
        }
      } else {
        stableReads = 0;
      }

      previousSize = size;
      previousModified = modified;
      sleep(interval);
    }

    throw new IOException("File was not stable before timeout: " + path);
  }

  private static void sleep(Duration interval) throws InterruptedException {
    Thread.sleep(Math.max(100L, interval.toMillis()));
  }
}
