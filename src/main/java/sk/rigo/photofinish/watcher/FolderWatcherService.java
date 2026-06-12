package sk.rigo.photofinish.watcher;

import sk.rigo.photofinish.config.AppSettings;
import sk.rigo.photofinish.repository.SettingsRepository;
import sk.rigo.photofinish.service.FileProcessingService;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FolderWatcherService implements AutoCloseable {

  private static final Logger LOGGER = Logger.getLogger(FolderWatcherService.class.getName());

  private final SettingsRepository settingsRepository;
  private final FileProcessingService processingService;
  private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
    Thread thread = new Thread(runnable, "photofinish-folder-watcher");
    thread.setDaemon(true);
    return thread;
  });
  private final AtomicBoolean running = new AtomicBoolean(false);
  private final List<Consumer<WatcherState>> listeners = new CopyOnWriteArrayList<>();

  private volatile WatchService watchService;
  private volatile Future<?> watchTask;
  private volatile WatcherState state = WatcherState.stopped();

  public FolderWatcherService(SettingsRepository settingsRepository, FileProcessingService processingService) {
    this.settingsRepository = settingsRepository;
    this.processingService = processingService;
  }

  public boolean isRunning() {
    return running.get();
  }

  public WatcherState state() {
    return state;
  }

  public void addStateListener(Consumer<WatcherState> listener) {
    listeners.add(listener);
  }

  public void start() {
    if (!running.compareAndSet(false, true)) {
      return;
    }
    watchTask = executor.submit(this::watchLoop);
  }

  public void stop() {
    running.set(false);
    WatchService current = watchService;
    if (current != null) {
      try {
        current.close();
      } catch (IOException ex) {
        LOGGER.log(Level.FINE, "Failed to close watcher", ex);
      }
    }
    setState(WatcherState.stopped());
  }

  @Override
  public void close() {
    stop();
    executor.shutdownNow();
  }

  private void watchLoop() {
    Path inputDirectory = null;
    try {
      AppSettings settings = settingsRepository.load();
      inputDirectory = Path.of(settings.getInputDirectory());
      Files.createDirectories(inputDirectory);
      watchService = inputDirectory.getFileSystem().newWatchService();
      inputDirectory.register(
          watchService,
          StandardWatchEventKinds.ENTRY_CREATE,
          StandardWatchEventKinds.ENTRY_MODIFY
      );

      setState(WatcherState.running(inputDirectory, "Watching"));
      enqueueExistingImages(inputDirectory);

      while (running.get()) {
        WatchKey key = watchService.take();
        for (WatchEvent<?> event : key.pollEvents()) {
          if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
            continue;
          }
          Path changed = inputDirectory.resolve((Path) event.context());
          if (isSupportedImage(changed)) {
            processingService.submit(changed);
          }
        }
        if (!key.reset()) {
          throw new IOException("Watch key is no longer valid for " + inputDirectory);
        }
      }
    } catch (ClosedWatchServiceException ignored) {
      running.set(false);
    } catch (IOException | InterruptedException | SQLException ex) {
      running.set(false);
      if (ex instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      String pathText = inputDirectory == null ? "" : inputDirectory.toString();
      LOGGER.log(Level.WARNING, "Folder watcher failed", ex);
      setState(WatcherState.error(inputDirectory, "Watcher stopped for " + pathText + ": " + ex.getMessage()));
    }
  }

  private void enqueueExistingImages(Path inputDirectory) throws IOException {
    List<Path> candidates = new ArrayList<>();
    try (var stream = Files.list(inputDirectory)) {
      stream.filter(FolderWatcherService::isSupportedImage).forEach(candidates::add);
    }
    for (Path candidate : candidates) {
      processingService.submit(candidate);
    }
  }

  private void setState(WatcherState state) {
    this.state = state;
    for (Consumer<WatcherState> listener : listeners) {
      listener.accept(state);
    }
  }

  public static boolean isSupportedImage(Path path) {
    String filename = path.getFileName() == null ? "" : path.getFileName().toString().toLowerCase(Locale.ROOT);
    return filename.endsWith(".jpg")
        || filename.endsWith(".jpeg")
        || filename.endsWith(".png");
  }
}
