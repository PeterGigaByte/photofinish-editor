package sk.rigo.photofinish.ui;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import sk.rigo.photofinish.config.AppSettings;
import sk.rigo.photofinish.model.BrandingTemplate;
import sk.rigo.photofinish.model.LogoPosition;
import sk.rigo.photofinish.model.OutputFormat;
import sk.rigo.photofinish.model.ProcessedFile;
import sk.rigo.photofinish.model.ProcessingError;
import sk.rigo.photofinish.model.ProcessingStatus;
import sk.rigo.photofinish.model.ProcessingSummary;
import sk.rigo.photofinish.service.AppContext;
import sk.rigo.photofinish.update.UpdateInfo;

import java.io.File;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainView {

  private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
      .withZone(ZoneId.systemDefault());

  private final AppContext context;
  private final ExecutorService uiExecutor = Executors.newCachedThreadPool(runnable -> {
    Thread thread = new Thread(runnable, "photofinish-ui-worker");
    thread.setDaemon(true);
    return thread;
  });

  private final BorderPane root = new BorderPane();
  private final Label watcherStatus = new Label();
  private final Label processedSummary = new Label();
  private final TextArea lastErrors = new TextArea();
  private final TableView<ProcessedFile> historyTable = new TableView<>();
  private final Label statusLine = new Label();

  private TextField inputDirectoryField;
  private TextField exportDirectoryField;
  private TextField latestJsonUrlField;
  private CheckBox autoStartWatcherField;

  private TextField templateNameField;
  private TextField logoPathField;
  private ComboBox<LogoPosition> logoPositionField;
  private Slider logoScaleField;
  private Slider logoOpacityField;
  private Spinner<Integer> offsetXField;
  private Spinner<Integer> offsetYField;
  private CheckBox textBarEnabledField;
  private TextField textTemplateField;
  private Slider textBarHeightField;
  private TextField textBarColorField;
  private TextField textColorField;
  private TextField fontNameField;
  private Spinner<Integer> fontSizeField;
  private ComboBox<OutputFormat> outputFormatField;

  private UpdateInfo latestUpdateInfo;
  private Label updateStatusLabel;
  private Label updateVersionLabel;
  private TextArea updateNotesArea;
  private Button downloadUpdateButton;

  public MainView(AppContext context) {
    this.context = context;
    build();
    context.watcherService().addStateListener(state -> Platform.runLater(this::refreshDashboard));
    context.processingService().addListener(() -> Platform.runLater(() -> {
      refreshDashboard();
      refreshHistory();
    }));
    refreshAll();
    if (loadSettings().isAutoStartWatcher()) {
      context.watcherService().start();
    }
  }

  public Parent root() {
    return root;
  }

  private void build() {
    root.setTop(header());
    root.setCenter(tabs());
    root.setBottom(statusLine);
    BorderPane.setMargin(statusLine, new Insets(6, 12, 8, 12));
    statusLine.setText("App data: " + context.paths().dataDirectory());
  }

  private Parent header() {
    Label title = new Label(context.metadata().name());
    title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
    Label version = new Label("Version " + context.metadata().version());
    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);
    HBox box = new HBox(12, title, version, spacer);
    box.setAlignment(Pos.CENTER_LEFT);
    box.setPadding(new Insets(12));
    box.setStyle("-fx-background-color: #f4f6f8; -fx-border-color: transparent transparent #d9dee5 transparent;");
    return box;
  }

  private TabPane tabs() {
    TabPane tabs = new TabPane();
    tabs.getTabs().add(tab("Dashboard", dashboard()));
    tabs.getTabs().add(tab("Folders", folders()));
    tabs.getTabs().add(tab("Template", templateEditor()));
    tabs.getTabs().add(tab("History", history()));
    tabs.getTabs().add(tab("Updates", updates()));
    return tabs;
  }

  private Tab tab(String title, Parent content) {
    Tab tab = new Tab(title, content);
    tab.setClosable(false);
    return tab;
  }

  private Parent dashboard() {
    Button start = new Button("Start watcher");
    start.setOnAction(event -> context.watcherService().start());
    Button stop = new Button("Stop watcher");
    stop.setOnAction(event -> context.watcherService().stop());
    Button refresh = new Button("Refresh");
    refresh.setOnAction(event -> refreshAll());

    lastErrors.setEditable(false);
    lastErrors.setPrefRowCount(8);
    lastErrors.setWrapText(true);

    VBox content = new VBox(
        14,
        labelBlock("Watcher status", watcherStatus),
        labelBlock("Processing summary", processedSummary),
        new HBox(8, start, stop, refresh),
        new Label("Recent errors"),
        lastErrors
    );
    content.setPadding(new Insets(16));
    return content;
  }

  private Parent folders() {
    inputDirectoryField = new TextField();
    exportDirectoryField = new TextField();
    latestJsonUrlField = new TextField();
    autoStartWatcherField = new CheckBox("Start watcher when app opens");

    Button browseInput = new Button("Browse");
    browseInput.setOnAction(event -> chooseDirectory(inputDirectoryField));
    Button browseExport = new Button("Browse");
    browseExport.setOnAction(event -> chooseDirectory(exportDirectoryField));
    Button save = new Button("Save settings");
    save.setOnAction(event -> saveSettings());

    GridPane grid = formGrid();
    grid.add(new Label("Input folder"), 0, 0);
    grid.add(inputDirectoryField, 1, 0);
    grid.add(browseInput, 2, 0);
    grid.add(new Label("Export folder"), 0, 1);
    grid.add(exportDirectoryField, 1, 1);
    grid.add(browseExport, 2, 1);
    grid.add(new Label("Latest JSON URL"), 0, 2);
    grid.add(latestJsonUrlField, 1, 2, 2, 1);
    grid.add(autoStartWatcherField, 1, 3, 2, 1);
    grid.add(save, 1, 4);

    VBox content = new VBox(12, sectionTitle("Folder settings"), grid);
    content.setPadding(new Insets(16));
    return content;
  }

  private Parent templateEditor() {
    templateNameField = new TextField();
    logoPathField = new TextField();
    logoPositionField = new ComboBox<>(FXCollections.observableArrayList(LogoPosition.values()));
    logoScaleField = slider(1, 60, 18);
    logoOpacityField = slider(0, 1, 0.85);
    offsetXField = spinner(-1000, 1000, 24);
    offsetYField = spinner(-1000, 1000, 24);
    textBarEnabledField = new CheckBox("Show bottom text bar");
    textTemplateField = new TextField();
    textBarHeightField = slider(3, 25, 8);
    textBarColorField = new TextField();
    textColorField = new TextField();
    fontNameField = new TextField();
    fontSizeField = spinner(8, 160, 32);
    outputFormatField = new ComboBox<>(FXCollections.observableArrayList(OutputFormat.values()));

    Button browseLogo = new Button("Browse");
    browseLogo.setOnAction(event -> chooseLogo());
    Button save = new Button("Save template");
    save.setOnAction(event -> saveTemplate());

    GridPane grid = formGrid();
    int row = 0;
    grid.add(new Label("Template name"), 0, row);
    grid.add(templateNameField, 1, row++, 2, 1);
    grid.add(new Label("Logo file"), 0, row);
    grid.add(logoPathField, 1, row);
    grid.add(browseLogo, 2, row++);
    grid.add(new Label("Logo position"), 0, row);
    grid.add(logoPositionField, 1, row++, 2, 1);
    grid.add(new Label("Logo size (% width)"), 0, row);
    grid.add(logoScaleField, 1, row++, 2, 1);
    grid.add(new Label("Logo opacity"), 0, row);
    grid.add(logoOpacityField, 1, row++, 2, 1);
    grid.add(new Label("X offset"), 0, row);
    grid.add(offsetXField, 1, row++, 2, 1);
    grid.add(new Label("Y offset"), 0, row);
    grid.add(offsetYField, 1, row++, 2, 1);
    grid.add(textBarEnabledField, 1, row++, 2, 1);
    grid.add(new Label("Text template"), 0, row);
    grid.add(textTemplateField, 1, row++, 2, 1);
    grid.add(new Label("Text bar height (%)"), 0, row);
    grid.add(textBarHeightField, 1, row++, 2, 1);
    grid.add(new Label("Text bar color"), 0, row);
    grid.add(textBarColorField, 1, row++, 2, 1);
    grid.add(new Label("Text color"), 0, row);
    grid.add(textColorField, 1, row++, 2, 1);
    grid.add(new Label("Font"), 0, row);
    grid.add(fontNameField, 1, row++, 2, 1);
    grid.add(new Label("Font size"), 0, row);
    grid.add(fontSizeField, 1, row++, 2, 1);
    grid.add(new Label("Output format"), 0, row);
    grid.add(outputFormatField, 1, row++, 2, 1);
    grid.add(save, 1, row);

    VBox content = new VBox(12, sectionTitle("Branding template"), grid);
    content.setPadding(new Insets(16));
    ScrollPane scrollPane = new ScrollPane(content);
    scrollPane.setFitToWidth(true);
    return scrollPane;
  }

  private Parent history() {
    TableColumn<ProcessedFile, String> idColumn = new TableColumn<>("ID");
    idColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(Long.toString(data.getValue().id())));
    TableColumn<ProcessedFile, String> sourceColumn = new TableColumn<>("Source");
    sourceColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().sourcePath()));
    TableColumn<ProcessedFile, String> statusColumn = new TableColumn<>("Status");
    statusColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().status().name()));
    TableColumn<ProcessedFile, String> outputColumn = new TableColumn<>("Output");
    outputColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(nullToEmpty(data.getValue().outputPath())));
    TableColumn<ProcessedFile, String> processedColumn = new TableColumn<>("Processed");
    processedColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(formatInstant(data.getValue().processedAt())));
    TableColumn<ProcessedFile, String> messageColumn = new TableColumn<>("Message");
    messageColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(nullToEmpty(data.getValue().message())));
    historyTable.getColumns().clear();
    historyTable.getColumns().add(idColumn);
    historyTable.getColumns().add(sourceColumn);
    historyTable.getColumns().add(statusColumn);
    historyTable.getColumns().add(outputColumn);
    historyTable.getColumns().add(processedColumn);
    historyTable.getColumns().add(messageColumn);

    Button refresh = new Button("Refresh");
    refresh.setOnAction(event -> refreshHistory());
    Button retryExport = new Button("Retry export");
    retryExport.setOnAction(event -> {
      ProcessedFile selected = historyTable.getSelectionModel().getSelectedItem();
      if (selected != null && selected.status() == ProcessingStatus.PENDING_EXPORT) {
        context.processingService().retryExport(selected.id());
      }
    });

    VBox content = new VBox(10, new HBox(8, refresh, retryExport), historyTable);
    content.setPadding(new Insets(16));
    VBox.setVgrow(historyTable, Priority.ALWAYS);
    return content;
  }

  private Parent updates() {
    updateStatusLabel = new Label("No update check has been run.");
    updateVersionLabel = new Label();
    updateNotesArea = new TextArea();
    updateNotesArea.setEditable(false);
    updateNotesArea.setWrapText(true);
    updateNotesArea.setPrefRowCount(8);

    Button checkUpdate = new Button("Check for updates");
    checkUpdate.setOnAction(event -> checkForUpdates());
    downloadUpdateButton = new Button("Download installer");
    downloadUpdateButton.setDisable(true);
    downloadUpdateButton.setOnAction(event -> downloadUpdate());

    VBox content = new VBox(
        12,
        sectionTitle("Updates"),
        updateStatusLabel,
        updateVersionLabel,
        new HBox(8, checkUpdate, downloadUpdateButton),
        new Label("Release notes"),
        updateNotesArea
    );
    content.setPadding(new Insets(16));
    return content;
  }

  private void refreshAll() {
    loadSettingsIntoFields();
    loadTemplateIntoFields();
    refreshDashboard();
    refreshHistory();
  }

  private void refreshDashboard() {
    watcherStatus.setText(context.watcherService().state().message());
    CompletableFuture
        .supplyAsync(() -> {
          try {
            ProcessingSummary summary = context.processedFileRepository().summary();
            List<ProcessingError> errors = context.errorRepository().listRecent(5);
            return new DashboardData(summary, errors);
          } catch (SQLException ex) {
            throw new IllegalStateException(ex);
          }
        }, uiExecutor)
        .thenAccept(data -> Platform.runLater(() -> {
          processedSummary.setText(
              "Total: " + data.summary().total()
                  + " | Exported: " + data.summary().exported()
                  + " | Pending export: " + data.summary().pendingExport()
                  + " | Failed: " + data.summary().failed()
          );
          StringBuilder builder = new StringBuilder();
          for (ProcessingError error : data.errors()) {
            builder
                .append(formatInstant(error.createdAt()))
                .append(" [")
                .append(error.stage())
                .append("] ")
                .append(error.message())
                .append(System.lineSeparator());
          }
          lastErrors.setText(builder.toString());
        }))
        .exceptionally(ex -> {
          Platform.runLater(() -> statusLine.setText("Dashboard refresh failed: " + ex.getMessage()));
          return null;
        });
  }

  private void refreshHistory() {
    CompletableFuture
        .supplyAsync(() -> {
          try {
            return context.processedFileRepository().listRecent(200);
          } catch (SQLException ex) {
            throw new IllegalStateException(ex);
          }
        }, uiExecutor)
        .thenAccept(files -> Platform.runLater(() -> historyTable.setItems(FXCollections.observableArrayList(files))))
        .exceptionally(ex -> {
          Platform.runLater(() -> statusLine.setText("History refresh failed: " + ex.getMessage()));
          return null;
        });
  }

  private void loadSettingsIntoFields() {
    AppSettings settings = loadSettings();
    inputDirectoryField.setText(settings.getInputDirectory());
    exportDirectoryField.setText(settings.getExportDirectory());
    latestJsonUrlField.setText(settings.getLatestJsonUrl());
    autoStartWatcherField.setSelected(settings.isAutoStartWatcher());
  }

  private void saveSettings() {
    String inputDirectory = inputDirectoryField.getText().trim();
    String exportDirectory = exportDirectoryField.getText().trim();
    String latestJsonUrl = latestJsonUrlField.getText().trim();
    boolean autoStartWatcher = autoStartWatcherField.isSelected();

    CompletableFuture.runAsync(() -> {
      try {
        AppSettings settings = context.settingsRepository().load();
        settings.setInputDirectory(inputDirectory);
        settings.setExportDirectory(exportDirectory);
        settings.setLatestJsonUrl(latestJsonUrl);
        settings.setAutoStartWatcher(autoStartWatcher);
        context.settingsRepository().save(settings);
      } catch (SQLException ex) {
        throw new IllegalStateException(ex);
      }
    }, uiExecutor).thenRun(() -> Platform.runLater(() -> statusLine.setText("Settings saved.")))
        .exceptionally(ex -> {
          Platform.runLater(() -> statusLine.setText("Settings save failed: " + ex.getMessage()));
          return null;
        });
  }

  private void loadTemplateIntoFields() {
    CompletableFuture
        .supplyAsync(() -> {
          try {
            AppSettings settings = context.settingsRepository().load();
            return context.templateRepository().findById(settings.getActiveTemplateId())
                .orElseGet(() -> {
                  try {
                    return context.templateRepository().getOrCreateDefault();
                  } catch (SQLException ex) {
                    throw new IllegalStateException(ex);
                  }
                });
          } catch (SQLException ex) {
            throw new IllegalStateException(ex);
          }
        }, uiExecutor)
        .thenAccept(template -> Platform.runLater(() -> setTemplateFields(template)))
        .exceptionally(ex -> {
          Platform.runLater(() -> statusLine.setText("Template load failed: " + ex.getMessage()));
          return null;
        });
  }

  private void saveTemplate() {
    String templateName = templateNameField.getText().trim();
    String logoPath = logoPathField.getText().trim();
    LogoPosition logoPosition = logoPositionField.getValue();
    double logoScale = logoScaleField.getValue();
    double logoOpacity = logoOpacityField.getValue();
    int offsetX = offsetXField.getValue();
    int offsetY = offsetYField.getValue();
    boolean textBarEnabled = textBarEnabledField.isSelected();
    String textTemplate = textTemplateField.getText();
    double textBarHeight = textBarHeightField.getValue();
    String textBarColor = textBarColorField.getText().trim();
    String textColor = textColorField.getText().trim();
    String fontName = fontNameField.getText().trim();
    int fontSize = fontSizeField.getValue();
    OutputFormat outputFormat = outputFormatField.getValue();

    CompletableFuture
        .supplyAsync(() -> {
          try {
            AppSettings settings = context.settingsRepository().load();
            BrandingTemplate template = context.templateRepository().findById(settings.getActiveTemplateId())
                .orElseGet(BrandingTemplate::defaults);
            template.setName(templateName);
            template.setLogoPath(logoPath);
            template.setLogoPosition(logoPosition);
            template.setLogoScalePercent(logoScale);
            template.setLogoOpacity(logoOpacity);
            template.setOffsetX(offsetX);
            template.setOffsetY(offsetY);
            template.setTextBarEnabled(textBarEnabled);
            template.setTextTemplate(textTemplate);
            template.setTextBarHeightPercent(textBarHeight);
            template.setTextBarColor(textBarColor);
            template.setTextColor(textColor);
            template.setFontName(fontName);
            template.setFontSize(fontSize);
            template.setOutputFormat(outputFormat);
            BrandingTemplate saved = context.templateRepository().save(template);
            if (settings.getActiveTemplateId() == 0L) {
              settings.setActiveTemplateId(saved.getId());
              context.settingsRepository().save(settings);
            }
            return saved;
          } catch (SQLException ex) {
            throw new IllegalStateException(ex);
          }
        }, uiExecutor)
        .thenAccept(template -> Platform.runLater(() -> {
          setTemplateFields(template);
          statusLine.setText("Template saved.");
        }))
        .exceptionally(ex -> {
          Platform.runLater(() -> statusLine.setText("Template save failed: " + ex.getMessage()));
          return null;
        });
  }

  private void checkForUpdates() {
    updateStatusLabel.setText("Checking...");
    downloadUpdateButton.setDisable(true);
    CompletableFuture
        .supplyAsync(() -> {
          try {
            AppSettings settings = context.settingsRepository().load();
            return context.updateService().check(settings.getLatestJsonUrl(), context.metadata().version());
          } catch (Exception ex) {
            throw new IllegalStateException(ex);
          }
        }, uiExecutor)
        .thenAccept(info -> Platform.runLater(() -> {
          latestUpdateInfo = info;
          updateVersionLabel.setText("Latest version: " + info.version());
          updateNotesArea.setText(info.notes());
          if (info.available()) {
            updateStatusLabel.setText("Update available.");
            downloadUpdateButton.setDisable(false);
          } else {
            updateStatusLabel.setText("No update available.");
          }
        }))
        .exceptionally(ex -> {
          Platform.runLater(() -> updateStatusLabel.setText("Update check failed: " + ex.getMessage()));
          return null;
        });
  }

  private void downloadUpdate() {
    if (latestUpdateInfo == null) {
      return;
    }
    updateStatusLabel.setText("Downloading installer...");
    downloadUpdateButton.setDisable(true);
    CompletableFuture
        .supplyAsync(() -> {
          try {
            Path tempUpdateDirectory = Path.of(System.getProperty("java.io.tmpdir"), "photofinish-branding-studio-updates");
            return context.updateService().downloadInstaller(latestUpdateInfo, tempUpdateDirectory);
          } catch (Exception ex) {
            throw new IllegalStateException(ex);
          }
        }, uiExecutor)
        .thenAccept(path -> Platform.runLater(() -> updateStatusLabel.setText("Installer downloaded to " + path)))
        .exceptionally(ex -> {
          Platform.runLater(() -> {
            updateStatusLabel.setText("Download failed: " + ex.getMessage());
            downloadUpdateButton.setDisable(false);
          });
          return null;
        });
  }

  private AppSettings loadSettings() {
    try {
      return context.settingsRepository().load();
    } catch (SQLException ex) {
      throw new IllegalStateException(ex);
    }
  }

  private void setTemplateFields(BrandingTemplate template) {
    templateNameField.setText(template.getName());
    logoPathField.setText(nullToEmpty(template.getLogoPath()));
    logoPositionField.setValue(template.getLogoPosition());
    logoScaleField.setValue(template.getLogoScalePercent());
    logoOpacityField.setValue(template.getLogoOpacity());
    offsetXField.getValueFactory().setValue(template.getOffsetX());
    offsetYField.getValueFactory().setValue(template.getOffsetY());
    textBarEnabledField.setSelected(template.isTextBarEnabled());
    textTemplateField.setText(template.getTextTemplate());
    textBarHeightField.setValue(template.getTextBarHeightPercent());
    textBarColorField.setText(template.getTextBarColor());
    textColorField.setText(template.getTextColor());
    fontNameField.setText(template.getFontName());
    fontSizeField.getValueFactory().setValue(template.getFontSize());
    outputFormatField.setValue(template.getOutputFormat());
  }

  private void chooseDirectory(TextField field) {
    DirectoryChooser chooser = new DirectoryChooser();
    File current = field.getText().isBlank() ? null : new File(field.getText());
    if (current != null && current.isDirectory()) {
      chooser.setInitialDirectory(current);
    }
    File selected = chooser.showDialog(root.getScene().getWindow());
    if (selected != null) {
      field.setText(selected.toPath().toString());
    }
  }

  private void chooseLogo() {
    FileChooser chooser = new FileChooser();
    chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
    File selected = chooser.showOpenDialog(root.getScene().getWindow());
    if (selected != null) {
      logoPathField.setText(selected.toPath().toString());
    }
  }

  private static GridPane formGrid() {
    GridPane grid = new GridPane();
    grid.setHgap(10);
    grid.setVgap(10);
    grid.getColumnConstraints().add(new javafx.scene.layout.ColumnConstraints(150));
    javafx.scene.layout.ColumnConstraints mainColumn = new javafx.scene.layout.ColumnConstraints();
    mainColumn.setHgrow(Priority.ALWAYS);
    grid.getColumnConstraints().add(mainColumn);
    return grid;
  }

  private static Slider slider(double min, double max, double value) {
    Slider slider = new Slider(min, max, value);
    slider.setShowTickLabels(true);
    slider.setShowTickMarks(true);
    slider.setMajorTickUnit((max - min) / 4.0);
    slider.setBlockIncrement((max - min) / 20.0);
    return slider;
  }

  private static Spinner<Integer> spinner(int min, int max, int value) {
    Spinner<Integer> spinner = new Spinner<>();
    spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(min, max, value));
    spinner.setEditable(true);
    return spinner;
  }

  private static Parent labelBlock(String title, Label value) {
    Label titleLabel = sectionTitle(title);
    value.setStyle("-fx-font-size: 15px;");
    return new VBox(4, titleLabel, value);
  }

  private static Label sectionTitle(String text) {
    Label label = new Label(text);
    label.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
    return label;
  }

  private static String formatInstant(java.time.Instant instant) {
    return instant == null ? "" : DATE_TIME.format(instant);
  }

  private static String nullToEmpty(String value) {
    return value == null ? "" : value;
  }

  private record DashboardData(ProcessingSummary summary, List<ProcessingError> errors) {
  }
}
