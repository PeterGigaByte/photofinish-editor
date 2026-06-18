package sk.rigo.photofinish.service;

import sk.rigo.photofinish.config.AppMetadata;
import sk.rigo.photofinish.config.AppPaths;
import sk.rigo.photofinish.api.AthleticOfficeService;
import sk.rigo.photofinish.repository.BrandingTemplateRepository;
import sk.rigo.photofinish.repository.ProcessedFileRepository;
import sk.rigo.photofinish.repository.ProcessingErrorRepository;
import sk.rigo.photofinish.repository.SettingsRepository;
import sk.rigo.photofinish.update.UpdateService;
import sk.rigo.photofinish.watcher.FolderWatcherService;

public record AppContext(
    AppMetadata metadata,
    AppPaths paths,
    SettingsRepository settingsRepository,
    BrandingTemplateRepository templateRepository,
    ProcessedFileRepository processedFileRepository,
    ProcessingErrorRepository errorRepository,
    FileProcessingService processingService,
    FolderWatcherService watcherService,
    AthleticOfficeService athleticOfficeService,
    UpdateService updateService
) implements AutoCloseable {

  @Override
  public void close() {
    watcherService.close();
    processingService.close();
  }
}
