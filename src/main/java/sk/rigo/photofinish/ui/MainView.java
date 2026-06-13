package sk.rigo.photofinish.ui;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import sk.rigo.photofinish.config.AppSettings;
import sk.rigo.photofinish.image.BrandingRenderer;
import sk.rigo.photofinish.model.BrandingTemplate;
import sk.rigo.photofinish.model.ImageFitMode;
import sk.rigo.photofinish.model.LogoPosition;
import sk.rigo.photofinish.model.OutputFormat;
import sk.rigo.photofinish.model.ProcessedFile;
import sk.rigo.photofinish.model.ProcessingError;
import sk.rigo.photofinish.model.ProcessingStatus;
import sk.rigo.photofinish.model.ProcessingSummary;
import sk.rigo.photofinish.service.AppContext;
import sk.rigo.photofinish.update.UpdateInfo;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
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
  private CheckBox canvasEnabledField;
  private Spinner<Integer> canvasWidthField;
  private Spinner<Integer> canvasHeightField;
  private ComboBox<ImageFitMode> imageFitModeField;
  private CheckBox autoCropEnabledField;
  private TextField canvasBackgroundColorField;
  private CheckBox headerEnabledField;
  private Slider headerHeightField;
  private TextField headerBackgroundColorField;
  private TextField headerTextColorField;
  private TextField headerTitleField;
  private TextField headerSubtitleField;
  private TextField headerLeftLogoPathField;
  private TextField headerRightLogoPathField;
  private CheckBox resultsEnabledField;
  private Slider resultsHeightField;
  private TextField resultsTitleField;
  private TextArea resultsRowsField;
  private TextField resultsBackgroundColorField;
  private TextField resultsHeaderColorField;
  private TextField resultsAccentColorField;
  private TextField previewImagePathField;
  private ImageView templatePreviewImage;
  private Label templatePreviewStatus;

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
    root.getStyleClass().add("app-root");
    root.setTop(header());
    root.setCenter(tabs());
    root.setBottom(statusLine);
    BorderPane.setMargin(statusLine, new Insets(6, 12, 8, 12));
    statusLine.setText("App data: " + context.paths().dataDirectory());
  }

  private Parent header() {
    Label title = new Label(context.metadata().name());
    title.getStyleClass().add("app-title");
    Label version = new Label("Version " + context.metadata().version());
    version.getStyleClass().add("app-version");
    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);
    HBox box = new HBox(12, title, version, spacer);
    box.setAlignment(Pos.CENTER_LEFT);
    box.setPadding(new Insets(12));
    box.getStyleClass().add("app-header");
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
    Button start = new Button("▶ Start watcher");
    start.setOnAction(event -> context.watcherService().start());
    Button stop = new Button("■ Stop watcher");
    stop.setOnAction(event -> context.watcherService().stop());
    Button refresh = new Button("⟳ Refresh");
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

    Button browseInput = new Button("▣ Browse");
    browseInput.setOnAction(event -> chooseDirectory(inputDirectoryField));
    Button browseExport = new Button("▣ Browse");
    browseExport.setOnAction(event -> chooseDirectory(exportDirectoryField));
    Button save = new Button("✓ Save settings");
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
    canvasEnabledField = new CheckBox("Use poster canvas");
    canvasWidthField = spinner(320, 5000, 1080);
    canvasHeightField = spinner(320, 5000, 1920);
    imageFitModeField = new ComboBox<>(FXCollections.observableArrayList(ImageFitMode.values()));
    autoCropEnabledField = new CheckBox("Auto-crop empty (no athlete) areas of long strips");
    canvasBackgroundColorField = new TextField();
    headerEnabledField = new CheckBox("Show top header");
    headerHeightField = slider(5, 30, 10);
    headerBackgroundColorField = new TextField();
    headerTextColorField = new TextField();
    headerTitleField = new TextField();
    headerSubtitleField = new TextField();
    headerLeftLogoPathField = new TextField();
    headerRightLogoPathField = new TextField();
    resultsEnabledField = new CheckBox("Show results table");
    resultsHeightField = slider(10, 45, 24);
    resultsTitleField = new TextField();
    resultsRowsField = new TextArea();
    resultsRowsField.setPrefRowCount(5);
    resultsRowsField.setWrapText(false);
    resultsBackgroundColorField = new TextField();
    resultsHeaderColorField = new TextField();
    resultsAccentColorField = new TextField();
    previewImagePathField = new TextField();
    previewImagePathField.setEditable(false);
    previewImagePathField.setPromptText("Default preview image");
    templatePreviewImage = new ImageView();
    templatePreviewImage.setPreserveRatio(true);
    templatePreviewImage.setFitWidth(520);
    templatePreviewImage.setFitHeight(340);
    templatePreviewStatus = new Label("Default preview image");

    Button browseLogo = new Button("▣ Browse");
    browseLogo.setOnAction(event -> chooseImageInto(logoPathField));
    Button browseLeftHeaderLogo = new Button("▣ Browse");
    browseLeftHeaderLogo.setOnAction(event -> chooseImageInto(headerLeftLogoPathField));
    Button browseRightHeaderLogo = new Button("▣ Browse");
    browseRightHeaderLogo.setOnAction(event -> chooseImageInto(headerRightLogoPathField));
    Button save = new Button("✓ Save template");
    save.setOnAction(event -> saveTemplate());
    Button choosePreview = new Button("▣ Choose sample");
    choosePreview.setOnAction(event -> choosePreviewImage());
    Button defaultPreview = new Button("↺ Use default");
    defaultPreview.setOnAction(event -> {
      previewImagePathField.clear();
      refreshTemplatePreview();
    });
    Button refreshPreview = new Button("⟳ Refresh preview");
    refreshPreview.setOnAction(event -> refreshTemplatePreview());

    GridPane grid = formGrid();
    int row = 0;
    grid.add(new Label("Template name"), 0, row);
    grid.add(templateNameField, 1, row++, 2, 1);

    grid.add(sectionTitle("Poster canvas"), 0, row++, 3, 1);
    grid.add(canvasEnabledField, 1, row++, 2, 1);
    grid.add(new Label("Canvas size"), 0, row);
    grid.add(new HBox(8, canvasWidthField, new Label("x"), canvasHeightField), 1, row++, 2, 1);
    grid.add(new Label("Image fit"), 0, row);
    grid.add(imageFitModeField, 1, row++, 2, 1);
    grid.add(autoCropEnabledField, 1, row++, 2, 1);
    grid.add(new Label("Canvas background"), 0, row);
    grid.add(canvasBackgroundColorField, 1, row++, 2, 1);

    grid.add(sectionTitle("Top header"), 0, row++, 3, 1);
    grid.add(headerEnabledField, 1, row++, 2, 1);
    grid.add(new Label("Header height (%)"), 0, row);
    grid.add(headerHeightField, 1, row++, 2, 1);
    grid.add(new Label("Header background"), 0, row);
    grid.add(headerBackgroundColorField, 1, row++, 2, 1);
    grid.add(new Label("Header text color"), 0, row);
    grid.add(headerTextColorField, 1, row++, 2, 1);
    grid.add(new Label("Header title"), 0, row);
    grid.add(headerTitleField, 1, row++, 2, 1);
    grid.add(new Label("Header subtitle"), 0, row);
    grid.add(headerSubtitleField, 1, row++, 2, 1);
    grid.add(new Label("Left header logo"), 0, row);
    grid.add(headerLeftLogoPathField, 1, row);
    grid.add(browseLeftHeaderLogo, 2, row++);
    grid.add(new Label("Right header logo"), 0, row);
    grid.add(headerRightLogoPathField, 1, row);
    grid.add(browseRightHeaderLogo, 2, row++);

    grid.add(sectionTitle("Overlay logo"), 0, row++, 3, 1);
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

    grid.add(sectionTitle("Bottom text bar"), 0, row++, 3, 1);
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

    grid.add(sectionTitle("Results table"), 0, row++, 3, 1);
    grid.add(resultsEnabledField, 1, row++, 2, 1);
    grid.add(new Label("Results height (%)"), 0, row);
    grid.add(resultsHeightField, 1, row++, 2, 1);
    grid.add(new Label("Results title"), 0, row);
    grid.add(resultsTitleField, 1, row++, 2, 1);
    grid.add(new Label("Rows"), 0, row);
    grid.add(resultsRowsField, 1, row++, 2, 1);
    grid.add(new Label("Results background"), 0, row);
    grid.add(resultsBackgroundColorField, 1, row++, 2, 1);
    grid.add(new Label("Results header"), 0, row);
    grid.add(resultsHeaderColorField, 1, row++, 2, 1);
    grid.add(new Label("Result accent"), 0, row);
    grid.add(resultsAccentColorField, 1, row++, 2, 1);

    grid.add(sectionTitle("Export"), 0, row++, 3, 1);
    grid.add(new Label("Output format"), 0, row);
    grid.add(outputFormatField, 1, row++, 2, 1);
    grid.add(save, 1, row);

    HBox previewActions = new HBox(8, previewImagePathField, choosePreview, defaultPreview, refreshPreview);
    HBox.setHgrow(previewImagePathField, Priority.ALWAYS);
    VBox preview = new VBox(
        10,
        sectionTitle("Preview"),
        templatePreviewStatus,
        previewActions,
        previewFrame(templatePreviewImage)
    );

    VBox content = new VBox(18, sectionTitle("Branding template"), grid, preview);
    content.setPadding(new Insets(16));
    ScrollPane scrollPane = new ScrollPane(content);
    scrollPane.setFitToWidth(true);
    Platform.runLater(this::refreshTemplatePreview);
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

    Button refresh = new Button("⟳ Refresh");
    refresh.setOnAction(event -> refreshHistory());
    Button retryExport = new Button("↻ Retry export");
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

    Button checkUpdate = new Button("⟳ Check for updates");
    checkUpdate.setOnAction(event -> checkForUpdates());
    downloadUpdateButton = new Button("⬇ Download and install");
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
          Platform.runLater(() -> statusLine.setText("Dashboard refresh failed: " + userMessage(ex)));
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
          Platform.runLater(() -> statusLine.setText("History refresh failed: " + userMessage(ex)));
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
          Platform.runLater(() -> statusLine.setText("Settings save failed: " + userMessage(ex)));
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
          Platform.runLater(() -> statusLine.setText("Template load failed: " + userMessage(ex)));
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
    boolean canvasEnabled = canvasEnabledField.isSelected();
    int canvasWidth = canvasWidthField.getValue();
    int canvasHeight = canvasHeightField.getValue();
    ImageFitMode imageFitMode = imageFitModeField.getValue();
    boolean autoCropEnabled = autoCropEnabledField.isSelected();
    String canvasBackgroundColor = canvasBackgroundColorField.getText().trim();
    boolean headerEnabled = headerEnabledField.isSelected();
    double headerHeight = headerHeightField.getValue();
    String headerBackgroundColor = headerBackgroundColorField.getText().trim();
    String headerTextColor = headerTextColorField.getText().trim();
    String headerTitle = headerTitleField.getText();
    String headerSubtitle = headerSubtitleField.getText();
    String headerLeftLogoPath = headerLeftLogoPathField.getText().trim();
    String headerRightLogoPath = headerRightLogoPathField.getText().trim();
    boolean resultsEnabled = resultsEnabledField.isSelected();
    double resultsHeight = resultsHeightField.getValue();
    String resultsTitle = resultsTitleField.getText();
    String resultsRows = resultsRowsField.getText();
    String resultsBackgroundColor = resultsBackgroundColorField.getText().trim();
    String resultsHeaderColor = resultsHeaderColorField.getText().trim();
    String resultsAccentColor = resultsAccentColorField.getText().trim();

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
            template.setCanvasEnabled(canvasEnabled);
            template.setCanvasWidth(canvasWidth);
            template.setCanvasHeight(canvasHeight);
            template.setImageFitMode(valueOrDefault(imageFitMode, ImageFitMode.ORIGINAL));
            template.setAutoCropEnabled(autoCropEnabled);
            template.setCanvasBackgroundColor(canvasBackgroundColor);
            template.setHeaderEnabled(headerEnabled);
            template.setHeaderHeightPercent(headerHeight);
            template.setHeaderBackgroundColor(headerBackgroundColor);
            template.setHeaderTextColor(headerTextColor);
            template.setHeaderTitle(headerTitle);
            template.setHeaderSubtitle(headerSubtitle);
            template.setHeaderLeftLogoPath(headerLeftLogoPath);
            template.setHeaderRightLogoPath(headerRightLogoPath);
            template.setResultsEnabled(resultsEnabled);
            template.setResultsHeightPercent(resultsHeight);
            template.setResultsTitle(resultsTitle);
            template.setResultsRowsText(resultsRows);
            template.setResultsBackgroundColor(resultsBackgroundColor);
            template.setResultsHeaderColor(resultsHeaderColor);
            template.setResultsAccentColor(resultsAccentColor);
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
          Platform.runLater(() -> statusLine.setText("Template save failed: " + userMessage(ex)));
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
          Platform.runLater(() -> updateStatusLabel.setText("Update check failed: " + userMessage(ex)));
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
            Path installerPath = context.updateService().downloadInstaller(latestUpdateInfo, tempUpdateDirectory);
            context.updateService().launchInstallerAfterCurrentAppExits(installerPath);
            return installerPath;
          } catch (Exception ex) {
            throw new IllegalStateException(ex);
          }
        }, uiExecutor)
        .thenAccept(path -> Platform.runLater(() -> {
          updateStatusLabel.setText("Installer downloaded. Closing app and starting installer.");
          Platform.exit();
        }))
        .exceptionally(ex -> {
          Platform.runLater(() -> {
            updateStatusLabel.setText("Download failed: " + userMessage(ex));
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
    canvasEnabledField.setSelected(template.isCanvasEnabled());
    canvasWidthField.getValueFactory().setValue(template.getCanvasWidth());
    canvasHeightField.getValueFactory().setValue(template.getCanvasHeight());
    imageFitModeField.setValue(valueOrDefault(template.getImageFitMode(), ImageFitMode.ORIGINAL));
    autoCropEnabledField.setSelected(template.isAutoCropEnabled());
    canvasBackgroundColorField.setText(template.getCanvasBackgroundColor());
    headerEnabledField.setSelected(template.isHeaderEnabled());
    headerHeightField.setValue(template.getHeaderHeightPercent());
    headerBackgroundColorField.setText(template.getHeaderBackgroundColor());
    headerTextColorField.setText(template.getHeaderTextColor());
    headerTitleField.setText(template.getHeaderTitle());
    headerSubtitleField.setText(template.getHeaderSubtitle());
    headerLeftLogoPathField.setText(nullToEmpty(template.getHeaderLeftLogoPath()));
    headerRightLogoPathField.setText(nullToEmpty(template.getHeaderRightLogoPath()));
    resultsEnabledField.setSelected(template.isResultsEnabled());
    resultsHeightField.setValue(template.getResultsHeightPercent());
    resultsTitleField.setText(template.getResultsTitle());
    resultsRowsField.setText(template.getResultsRowsText());
    resultsBackgroundColorField.setText(template.getResultsBackgroundColor());
    resultsHeaderColorField.setText(template.getResultsHeaderColor());
    resultsAccentColorField.setText(template.getResultsAccentColor());
    if (templatePreviewImage != null) {
      refreshTemplatePreview();
    }
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
    chooseImageInto(logoPathField);
  }

  private void chooseImageInto(TextField field) {
    FileChooser chooser = new FileChooser();
    chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
    File selected = chooser.showOpenDialog(root.getScene().getWindow());
    if (selected != null) {
      field.setText(selected.toPath().toString());
    }
  }

  private void choosePreviewImage() {
    FileChooser chooser = new FileChooser();
    chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
    File selected = chooser.showOpenDialog(root.getScene().getWindow());
    if (selected != null) {
      previewImagePathField.setText(selected.toPath().toString());
      refreshTemplatePreview();
    }
  }

  private void refreshTemplatePreview() {
    BrandingTemplate template = templateFromFields();
    String previewPath = previewImagePathField.getText();
    templatePreviewStatus.setText("Rendering preview...");

    CompletableFuture
        .supplyAsync(() -> {
          try {
            BufferedImage rendered;
            BrandingRenderer renderer = new BrandingRenderer();
            if (previewPath == null || previewPath.isBlank()) {
              rendered = renderer.render(defaultPreviewImage(), Path.of("preview-sample.jpg"), template);
            } else {
              rendered = renderer.render(Path.of(previewPath), template);
            }
            return toFxImage(rendered);
          } catch (Exception ex) {
            throw new IllegalStateException(ex);
          }
        }, uiExecutor)
        .thenAccept(image -> Platform.runLater(() -> {
          templatePreviewImage.setImage(image);
          templatePreviewStatus.setText(
              previewPath == null || previewPath.isBlank()
                  ? "Preview rendered from default sample."
                  : "Preview rendered from selected sample."
          );
        }))
        .exceptionally(ex -> {
          Platform.runLater(() -> {
            templatePreviewImage.setImage(null);
            templatePreviewStatus.setText("Preview failed: " + userMessage(ex));
          });
          return null;
        });
  }

  private BrandingTemplate templateFromFields() {
    BrandingTemplate template = BrandingTemplate.defaults();
    template.setName(templateNameField.getText().trim());
    template.setLogoPath(logoPathField.getText().trim());
    template.setLogoPosition(valueOrDefault(logoPositionField.getValue(), LogoPosition.BOTTOM_RIGHT));
    template.setLogoScalePercent(logoScaleField.getValue());
    template.setLogoOpacity(logoOpacityField.getValue());
    template.setOffsetX(offsetXField.getValue());
    template.setOffsetY(offsetYField.getValue());
    template.setTextBarEnabled(textBarEnabledField.isSelected());
    template.setTextTemplate(textTemplateField.getText());
    template.setTextBarHeightPercent(textBarHeightField.getValue());
    template.setTextBarColor(textBarColorField.getText().trim());
    template.setTextColor(textColorField.getText().trim());
    template.setFontName(fontNameField.getText().trim());
    template.setFontSize(fontSizeField.getValue());
    template.setOutputFormat(valueOrDefault(outputFormatField.getValue(), OutputFormat.JPG));
    template.setCanvasEnabled(canvasEnabledField.isSelected());
    template.setCanvasWidth(canvasWidthField.getValue());
    template.setCanvasHeight(canvasHeightField.getValue());
    template.setImageFitMode(valueOrDefault(imageFitModeField.getValue(), ImageFitMode.ORIGINAL));
    template.setAutoCropEnabled(autoCropEnabledField.isSelected());
    template.setCanvasBackgroundColor(canvasBackgroundColorField.getText().trim());
    template.setHeaderEnabled(headerEnabledField.isSelected());
    template.setHeaderHeightPercent(headerHeightField.getValue());
    template.setHeaderBackgroundColor(headerBackgroundColorField.getText().trim());
    template.setHeaderTextColor(headerTextColorField.getText().trim());
    template.setHeaderTitle(headerTitleField.getText());
    template.setHeaderSubtitle(headerSubtitleField.getText());
    template.setHeaderLeftLogoPath(headerLeftLogoPathField.getText().trim());
    template.setHeaderRightLogoPath(headerRightLogoPathField.getText().trim());
    template.setResultsEnabled(resultsEnabledField.isSelected());
    template.setResultsHeightPercent(resultsHeightField.getValue());
    template.setResultsTitle(resultsTitleField.getText());
    template.setResultsRowsText(resultsRowsField.getText());
    template.setResultsBackgroundColor(resultsBackgroundColorField.getText().trim());
    template.setResultsHeaderColor(resultsHeaderColorField.getText().trim());
    template.setResultsAccentColor(resultsAccentColorField.getText().trim());
    return template;
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
    value.getStyleClass().add("metric-value");
    return new VBox(4, titleLabel, value);
  }

  private static Node previewFrame(ImageView imageView) {
    BorderPane frame = new BorderPane(imageView);
    frame.setMinHeight(360);
    frame.setPrefHeight(380);
    frame.getStyleClass().add("preview-frame");
    BorderPane.setAlignment(imageView, Pos.CENTER);
    return frame;
  }

  private static Label sectionTitle(String text) {
    Label label = new Label(text);
    label.getStyleClass().add("section-title");
    return label;
  }

  private static String formatInstant(java.time.Instant instant) {
    return instant == null ? "" : DATE_TIME.format(instant);
  }

  private static String nullToEmpty(String value) {
    return value == null ? "" : value;
  }

  private static String userMessage(Throwable throwable) {
    Throwable current = throwable;
    while (current.getCause() != null
        && (current instanceof java.util.concurrent.CompletionException
        || current instanceof IllegalStateException)) {
      current = current.getCause();
    }

    String message = current.getMessage();
    if (message == null || message.isBlank()) {
      return current.getClass().getSimpleName();
    }
    return message;
  }

  private static <T> T valueOrDefault(T value, T defaultValue) {
    return value == null ? defaultValue : value;
  }

  private static Image toFxImage(BufferedImage image) throws IOException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    ImageIO.write(image, "png", outputStream);
    return new Image(new ByteArrayInputStream(outputStream.toByteArray()));
  }

  private static BufferedImage defaultPreviewImage() {
    int width = 1600;
    int height = 900;
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = image.createGraphics();
    try {
      graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      graphics.setPaint(new GradientPaint(0, 0, new Color(34, 44, 58), width, height, new Color(172, 182, 190)));
      graphics.fillRect(0, 0, width, height);

      graphics.setColor(new Color(235, 238, 242, 210));
      graphics.fillRect(0, height / 2 - 38, width, 76);
      graphics.setColor(new Color(35, 41, 48));
      graphics.setStroke(new BasicStroke(3f));
      graphics.drawLine(0, height / 2, width, height / 2);

      graphics.setColor(new Color(230, 35, 35));
      graphics.fillRect(width / 2 - 4, 70, 8, height - 140);
      graphics.setColor(new Color(255, 255, 255, 210));
      graphics.setFont(new Font("Arial", Font.BOLD, 46));
      graphics.drawString("PHOTO FINISH PREVIEW", 72, 96);

      graphics.setFont(new Font("Arial", Font.PLAIN, 30));
      graphics.drawString("Generated sample image", 72, 145);

      graphics.setFont(new Font("Arial", Font.BOLD, 96));
      graphics.setColor(new Color(255, 255, 255, 165));
      graphics.drawString("12.345", width - 430, height / 2 - 70);
    } finally {
      graphics.dispose();
    }
    return image;
  }

  private record DashboardData(ProcessingSummary summary, List<ProcessingError> errors) {
  }
}
