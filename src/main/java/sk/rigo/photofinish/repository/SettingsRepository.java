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
    settings.setLatestJsonUrl(values.getOrDefault("latestJsonUrl", settings.getLatestJsonUrl()));
    settings.setActiveTemplateId(parseLong(values.get("activeTemplateId"), settings.getActiveTemplateId()));
    settings.setAutoStartWatcher(Boolean.parseBoolean(values.getOrDefault("autoStartWatcher", "false")));

    if (values.isEmpty()) {
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
    }
  }

  private AppSettings defaults() {
    AppSettings settings = new AppSettings();
    settings.setInputDirectory(paths.defaultInputDirectory().toString());
    settings.setExportDirectory(paths.defaultExportDirectory().toString());
    settings.setLatestJsonUrl("https://example.com/photofinish/latest.json");
    settings.setActiveTemplateId(0L);
    settings.setAutoStartWatcher(false);
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
}
