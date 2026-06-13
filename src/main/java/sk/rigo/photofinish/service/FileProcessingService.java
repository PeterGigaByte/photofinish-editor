package sk.rigo.photofinish.service;

import sk.rigo.photofinish.config.AppSettings;
import sk.rigo.photofinish.image.BrandingRenderer;
import sk.rigo.photofinish.image.ExportResult;
import sk.rigo.photofinish.image.ImageExporter;
import sk.rigo.photofinish.model.BrandingTemplate;
import sk.rigo.photofinish.model.ProcessedFile;
import sk.rigo.photofinish.model.ProcessingStatus;
import sk.rigo.photofinish.repository.BrandingTemplateRepository;
import sk.rigo.photofinish.repository.ProcessedFileRepository;
import sk.rigo.photofinish.repository.ProcessingErrorRepository;
import sk.rigo.photofinish.repository.SettingsRepository;
import sk.rigo.photofinish.watcher.FolderWatcherService;
import sk.rigo.photofinish.watcher.StableFileDetector;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileProcessingService implements AutoCloseable {

  private static final Logger LOGGER = Logger.getLogger(FileProcessingService.class.getName());

  private final SettingsRepository settingsRepository;
  private final BrandingTemplateRepository templateRepository;
  private final ProcessedFileRepository processedFileRepository;
  private final ProcessingErrorRepository errorRepository;
  private final StableFileDetector stableFileDetector;
  private final BrandingRenderer brandingRenderer;
  private final ImageExporter imageExporter;
  private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
    Thread thread = new Thread(runnable, "photofinish-file-processor");
    thread.setDaemon(true);
    return thread;
  });
  private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

  public FileProcessingService(
      SettingsRepository settingsRepository,
      BrandingTemplateRepository templateRepository,
      ProcessedFileRepository processedFileRepository,
      ProcessingErrorRepository errorRepository,
      StableFileDetector stableFileDetector,
      BrandingRenderer brandingRenderer,
      ImageExporter imageExporter
  ) {
    this.settingsRepository = settingsRepository;
    this.templateRepository = templateRepository;
    this.processedFileRepository = processedFileRepository;
    this.errorRepository = errorRepository;
    this.stableFileDetector = stableFileDetector;
    this.brandingRenderer = brandingRenderer;
    this.imageExporter = imageExporter;
  }

  public void addListener(Runnable listener) {
    listeners.add(listener);
  }

  public void submit(Path sourcePath) {
    if (!FolderWatcherService.isSupportedImage(sourcePath)) {
      return;
    }

    executor.submit(() -> {
      try {
        Optional<Long> queued = processedFileRepository.queueForProcessing(sourcePath);
        queued.ifPresent(id -> process(id, sourcePath));
      } catch (Exception ex) {
        errorRepository.log(sourcePath.toString(), "QUEUE", ex.getMessage(), ex);
        LOGGER.log(Level.WARNING, "Failed to queue file " + sourcePath, ex);
        notifyListeners();
      }
    });
  }

  public void retryExport(long processedFileId) {
    executor.submit(() -> {
      try {
        Optional<ProcessedFile> optionalFile = processedFileRepository.findById(processedFileId);
        if (optionalFile.isEmpty()) {
          return;
        }
        ProcessedFile file = optionalFile.get();
        ExportResult result = imageExporter.retryExport(file);
        if (result.status() == ProcessingStatus.EXPORTED) {
          processedFileRepository.markExported(file.id(), result.outputPath(), result.stagedPath(), result.message());
        } else if (result.status() == ProcessingStatus.PENDING_EXPORT) {
          processedFileRepository.markPendingExport(file.id(), result.outputPath(), result.stagedPath(), result.message());
        } else {
          processedFileRepository.markFailed(file.id(), result.message());
        }
      } catch (Exception ex) {
        errorRepository.log(Long.toString(processedFileId), "RETRY_EXPORT", ex.getMessage(), ex);
        LOGGER.log(Level.WARNING, "Retry export failed for record " + processedFileId, ex);
      } finally {
        notifyListeners();
      }
    });
  }

  @Override
  public void close() {
    executor.shutdownNow();
  }

  private void process(long recordId, Path sourcePath) {
    try {
      AppSettings settings = settingsRepository.load();
      BrandingTemplate template = templateRepository.findById(settings.getActiveTemplateId())
          .orElseGet(() -> {
            try {
              return templateRepository.getOrCreateDefault();
            } catch (SQLException ex) {
              throw new IllegalStateException(ex);
            }
          });

      processedFileRepository.markProcessing(recordId, template.getId());
      notifyListeners();

      stableFileDetector.waitUntilStable(sourcePath, Duration.ofSeconds(45), Duration.ofMillis(600));
      // Record the fingerprint of the now-stable source so unchanged re-detections (incl. on restart)
      // are recognised as already handled, while genuinely new content at the same name re-exports.
      processedFileRepository.updateSourceFingerprint(
          recordId, Files.size(sourcePath), Files.getLastModifiedTime(sourcePath).toInstant());
      BufferedImage rendered = brandingRenderer.render(sourcePath, template);
      ExportResult result = imageExporter.export(rendered, sourcePath, settings, template);
      if (result.status() == ProcessingStatus.EXPORTED) {
        processedFileRepository.markExported(recordId, result.outputPath(), result.stagedPath(), result.message());
      } else {
        processedFileRepository.markPendingExport(recordId, result.outputPath(), result.stagedPath(), result.message());
      }
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      markFailed(recordId, sourcePath, "PROCESS", "Processing interrupted", ex);
    } catch (Exception ex) {
      markFailed(recordId, sourcePath, "PROCESS", ex.getMessage(), ex);
    } finally {
      notifyListeners();
    }
  }

  private void markFailed(long recordId, Path sourcePath, String stage, String message, Exception ex) {
    try {
      processedFileRepository.markFailed(recordId, message == null ? ex.getClass().getSimpleName() : message);
    } catch (SQLException sqlException) {
      LOGGER.log(Level.WARNING, "Failed to mark record failed", sqlException);
    }
    errorRepository.log(sourcePath.toString(), stage, message == null ? ex.getClass().getSimpleName() : message, ex);
    LOGGER.log(Level.WARNING, "Processing failed for " + sourcePath, ex);
  }

  private void notifyListeners() {
    for (Runnable listener : listeners) {
      listener.run();
    }
  }
}
