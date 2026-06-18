package sk.rigo.photofinish.repository;

import sk.rigo.photofinish.config.AppPaths;
import sk.rigo.photofinish.config.AppSettings;
import sk.rigo.photofinish.db.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class SettingsRepository {

  private static final String LEGACY_PLACEHOLDER_UPDATE_URL = "https://example.com/photofinish/latest.json";

  private final Database database;
  private final AppPaths paths;

  public SettingsRepository(Database database, AppPaths paths) {
    this.database = database;
    this.paths = paths;
  }

  public synchronized AppSettings load() throws SQLException {
    Map<String, String> values = new HashMap<>();
    try (Connection connection = database.getConnection();
         PreparedStatement statement = connection.prepareStatement("SELECT key, value FROM app_settings");
         ResultSet resultSet = statement.executeQuery()) {
      while (resultSet.next()) {
        values.put(resultSet.getString("key"), resultSet.getString("value"));
      }
    }

    AppSettings settings = defaults();
    settings.setInputDirectory(values.getOrDefault("inputDirectory", settings.getInputDirectory()));
    settings.setExportDirectory(values.getOrDefault("exportDirectory", settings.getExportDirectory()));
    String latestJsonUrl = values.getOrDefault("latestJsonUrl", settings.getLatestJsonUrl());
    settings.setLatestJsonUrl(normalizeLatestJsonUrl(latestJsonUrl));
    settings.setActiveTemplateId(parseLong(values.get("activeTemplateId"), settings.getActiveTemplateId()));
    settings.setAutoStartWatcher(Boolean.parseBoolean(values.getOrDefault("autoStartWatcher", "false")));
    settings.setAthleticOfficeApiEnabled(Boolean.parseBoolean(values.getOrDefault("athleticOfficeApiEnabled", "false")));
    settings.setAthleticOfficeBaseUrl(normalizeText(values.getOrDefault("athleticOfficeBaseUrl", settings.getAthleticOfficeBaseUrl())));
    settings.setAthleticOfficeActiveRaceId(normalizeText(values.getOrDefault("athleticOfficeActiveRaceId", settings.getAthleticOfficeActiveRaceId())));
    settings.setAthleticOfficeConnectionId(normalizeText(values.getOrDefault("athleticOfficeConnectionId", settings.getAthleticOfficeConnectionId())));

    if (values.isEmpty() || LEGACY_PLACEHOLDER_UPDATE_URL.equals(latestJsonUrl)) {
      save(settings);
    }
    return settings;
  }

  public synchronized void save(AppSettings settings) throws SQLException {
    try (Connection connection = database.getConnection()) {
      upsert(connection, "inputDirectory", settings.getInputDirectory());
      upsert(connection, "exportDirectory", settings.getExportDirectory());
      upsert(connection, "latestJsonUrl", settings.getLatestJsonUrl());
      upsert(connection, "activeTemplateId", Long.toString(settings.getActiveTemplateId()));
      upsert(connection, "autoStartWatcher", Boolean.toString(settings.isAutoStartWatcher()));
      upsert(connection, "athleticOfficeApiEnabled", Boolean.toString(settings.isAthleticOfficeApiEnabled()));
      upsert(connection, "athleticOfficeBaseUrl", settings.getAthleticOfficeBaseUrl());
      upsert(connection, "athleticOfficeActiveRaceId", settings.getAthleticOfficeActiveRaceId());
      upsert(connection, "athleticOfficeConnectionId", settings.getAthleticOfficeConnectionId());
    }
  }

  private AppSettings defaults() {
    AppSettings settings = new AppSettings();
    settings.setInputDirectory(paths.defaultInputDirectory().toString());
    settings.setExportDirectory(paths.defaultExportDirectory().toString());
    settings.setLatestJsonUrl("");
    settings.setActiveTemplateId(0L);
    settings.setAutoStartWatcher(false);
    settings.setAthleticOfficeApiEnabled(false);
    settings.setAthleticOfficeBaseUrl("");
    settings.setAthleticOfficeActiveRaceId("");
    settings.setAthleticOfficeConnectionId("");
    return settings;
  }

  private static void upsert(Connection connection, String key, String value) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement("""
        INSERT INTO app_settings(key, value)
        VALUES (?, ?)
        ON CONFLICT(key) DO UPDATE SET value = excluded.value
        """)) {
      statement.setString(1, key);
      statement.setString(2, value == null ? "" : value);
      statement.executeUpdate();
    }
  }

  private static long parseLong(String value, long defaultValue) {
    if (value == null || value.isBlank()) {
      return defaultValue;
    }
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException ex) {
      return defaultValue;
    }
  }

  private static String normalizeLatestJsonUrl(String value) {
    if (value == null || value.isBlank() || LEGACY_PLACEHOLDER_UPDATE_URL.equals(value)) {
      return "";
    }
    return value.trim();
  }

  private static String normalizeText(String value) {
    return value == null ? "" : value.trim();
  }
}
