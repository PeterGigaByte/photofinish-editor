package sk.rigo.photofinish;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import sk.rigo.photofinish.config.AppMetadata;
import sk.rigo.photofinish.config.AppPaths;
import sk.rigo.photofinish.config.AppSettings;
import sk.rigo.photofinish.config.AppLogger;
import sk.rigo.photofinish.api.AthleticOfficeService;
import sk.rigo.photofinish.db.Database;
import sk.rigo.photofinish.image.BrandingRenderer;
import sk.rigo.photofinish.image.ImageExporter;
import sk.rigo.photofinish.repository.BrandingTemplateRepository;
import sk.rigo.photofinish.repository.ProcessedFileRepository;
import sk.rigo.photofinish.repository.ProcessingErrorRepository;
import sk.rigo.photofinish.repository.SettingsRepository;
import sk.rigo.photofinish.service.AppContext;
import sk.rigo.photofinish.service.FileProcessingService;
import sk.rigo.photofinish.ui.AppIcons;
import sk.rigo.photofinish.ui.MainView;
import sk.rigo.photofinish.update.UpdateService;
import sk.rigo.photofinish.watcher.FolderWatcherService;
import sk.rigo.photofinish.watcher.StableFileDetector;

import java.util.logging.Logger;

public class MainApp extends Application {

  private static final Logger LOGGER = Logger.getLogger(MainApp.class.getName());

  private AppContext context;

  @Override
  public void init() throws Exception {
    AppPaths paths = AppPaths.create();
    AppLogger.configure(paths.logsDirectory());

    AppMetadata metadata = AppMetadata.load();
    Database database = new Database(paths.databaseFile());
    database.initialize();

    SettingsRepository settingsRepository = new SettingsRepository(database, paths);
    BrandingTemplateRepository templateRepository = new BrandingTemplateRepository(database);
    ProcessedFileRepository processedFileRepository = new ProcessedFileRepository(database);
    ProcessingErrorRepository errorRepository = new ProcessingErrorRepository(database);

    AppSettings settings = settingsRepository.load();
    long templateId = templateRepository.getOrCreateDefault().getId();
    if (settings.getActiveTemplateId() == 0L) {
      settings.setActiveTemplateId(templateId);
      settingsRepository.save(settings);
    }

    ImageExporter imageExporter = new ImageExporter(paths.stagingDirectory());
    AthleticOfficeService athleticOfficeService = new AthleticOfficeService();
    FileProcessingService processingService = new FileProcessingService(
        settingsRepository,
        templateRepository,
        processedFileRepository,
        errorRepository,
        new StableFileDetector(),
        athleticOfficeService,
        new BrandingRenderer(),
        imageExporter
    );
    FolderWatcherService watcherService = new FolderWatcherService(settingsRepository, processingService);
    UpdateService updateService = new UpdateService();

    context = new AppContext(
        metadata,
        paths,
        settingsRepository,
        templateRepository,
        processedFileRepository,
        errorRepository,
        processingService,
        watcherService,
        athleticOfficeService,
        updateService
    );

    LOGGER.info(() -> metadata.name() + " " + metadata.version() + " initialized at " + paths.dataDirectory());
  }

  @Override
  public void start(Stage stage) {
    MainView mainView = new MainView(context);
    Scene scene = new Scene(mainView.root(), 1120, 740);
    var stylesheet = MainApp.class.getResource("/styles/app.css");
    if (stylesheet != null) {
      scene.getStylesheets().add(stylesheet.toExternalForm());
    }
    stage.setTitle(context.metadata().name());
    stage.getIcons().addAll(AppIcons.windowIcons());
    stage.setMinWidth(980);
    stage.setMinHeight(640);
    stage.setScene(scene);
    stage.show();
  }

  @Override
  public void stop() {
    if (context != null) {
      context.close();
    }
  }

  public static void main(String[] args) {
    launch(args);
  }
}
