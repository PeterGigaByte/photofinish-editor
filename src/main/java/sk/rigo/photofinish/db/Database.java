package sk.rigo.photofinish.db;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {

  private final Path databaseFile;

  public Database(Path databaseFile) {
    this.databaseFile = databaseFile;
  }

  public Connection getConnection() throws SQLException {
    Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.toAbsolutePath());
    try (Statement statement = connection.createStatement()) {
      statement.execute("PRAGMA foreign_keys = ON");
      statement.execute("PRAGMA busy_timeout = 5000");
    }
    return connection;
  }

  public void initialize() throws SQLException {
    try (Connection connection = getConnection();
         Statement statement = connection.createStatement()) {
      statement.execute("""
          CREATE TABLE IF NOT EXISTS app_settings (
            key TEXT PRIMARY KEY,
            value TEXT NOT NULL
          )
          """);
      statement.execute("""
          CREATE TABLE IF NOT EXISTS branding_templates (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL,
            logo_path TEXT,
            logo_position TEXT NOT NULL,
            logo_scale_percent REAL NOT NULL,
            logo_opacity REAL NOT NULL,
            offset_x INTEGER NOT NULL,
            offset_y INTEGER NOT NULL,
            text_bar_enabled INTEGER NOT NULL,
            text_template TEXT NOT NULL,
            text_bar_height_percent REAL NOT NULL,
            text_bar_color TEXT NOT NULL,
            text_color TEXT NOT NULL,
            font_name TEXT NOT NULL,
            font_size INTEGER NOT NULL,
            output_format TEXT NOT NULL,
            canvas_enabled INTEGER NOT NULL DEFAULT 1,
            canvas_width INTEGER NOT NULL DEFAULT 1080,
            canvas_height INTEGER NOT NULL DEFAULT 1920,
            image_fit_mode TEXT NOT NULL DEFAULT 'COVER',
            auto_crop_enabled INTEGER NOT NULL DEFAULT 1,
            enhance_enabled INTEGER NOT NULL DEFAULT 1,
            canvas_background_color TEXT NOT NULL DEFAULT '#F6F8FA',
            header_enabled INTEGER NOT NULL DEFAULT 1,
            header_height_percent REAL NOT NULL DEFAULT 10,
            header_background_color TEXT NOT NULL DEFAULT '#0D5B91',
            header_text_color TEXT NOT NULL DEFAULT '#FFFFFF',
            header_title TEXT NOT NULL DEFAULT 'EVENT TITLE',
            header_subtitle TEXT NOT NULL DEFAULT 'Venue  |  Date',
            header_left_logo_path TEXT,
            header_right_logo_path TEXT,
            results_enabled INTEGER NOT NULL DEFAULT 1,
            results_height_percent REAL NOT NULL DEFAULT 24,
            results_title TEXT NOT NULL DEFAULT 'Race results',
            results_rows_text TEXT NOT NULL DEFAULT '',
            results_background_color TEXT NOT NULL DEFAULT '#203241',
            results_header_color TEXT NOT NULL DEFAULT '#172633',
            results_accent_color TEXT NOT NULL DEFAULT '#F0F500',
            updated_at TEXT NOT NULL
          )
          """);
      migrateBrandingTemplates(connection);
      statement.execute("""
          CREATE TABLE IF NOT EXISTS processed_files (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            source_path TEXT NOT NULL UNIQUE,
            source_size INTEGER NOT NULL,
            source_last_modified TEXT,
            output_path TEXT,
            staged_path TEXT,
            status TEXT NOT NULL,
            message TEXT,
            template_id INTEGER,
            created_at TEXT NOT NULL,
            processed_at TEXT
          )
          """);
      statement.execute("""
          CREATE TABLE IF NOT EXISTS processing_errors (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            source_path TEXT,
            stage TEXT NOT NULL,
            message TEXT NOT NULL,
            stack_trace TEXT,
            created_at TEXT NOT NULL
          )
          """);
    }
  }

  private static void migrateBrandingTemplates(Connection connection) throws SQLException {
    addColumnIfMissing(connection, "branding_templates", "canvas_enabled", "INTEGER NOT NULL DEFAULT 1");
    addColumnIfMissing(connection, "branding_templates", "canvas_width", "INTEGER NOT NULL DEFAULT 1080");
    addColumnIfMissing(connection, "branding_templates", "canvas_height", "INTEGER NOT NULL DEFAULT 1920");
    addColumnIfMissing(connection, "branding_templates", "image_fit_mode", "TEXT NOT NULL DEFAULT 'COVER'");
    addColumnIfMissing(connection, "branding_templates", "auto_crop_enabled", "INTEGER NOT NULL DEFAULT 1");
    addColumnIfMissing(connection, "branding_templates", "enhance_enabled", "INTEGER NOT NULL DEFAULT 1");
    addColumnIfMissing(connection, "branding_templates", "canvas_background_color", "TEXT NOT NULL DEFAULT '#F6F8FA'");
    addColumnIfMissing(connection, "branding_templates", "header_enabled", "INTEGER NOT NULL DEFAULT 1");
    addColumnIfMissing(connection, "branding_templates", "header_height_percent", "REAL NOT NULL DEFAULT 10");
    addColumnIfMissing(connection, "branding_templates", "header_background_color", "TEXT NOT NULL DEFAULT '#0D5B91'");
    addColumnIfMissing(connection, "branding_templates", "header_text_color", "TEXT NOT NULL DEFAULT '#FFFFFF'");
    addColumnIfMissing(connection, "branding_templates", "header_title", "TEXT NOT NULL DEFAULT 'EVENT TITLE'");
    addColumnIfMissing(connection, "branding_templates", "header_subtitle", "TEXT NOT NULL DEFAULT 'Venue  |  Date'");
    addColumnIfMissing(connection, "branding_templates", "header_left_logo_path", "TEXT");
    addColumnIfMissing(connection, "branding_templates", "header_right_logo_path", "TEXT");
    addColumnIfMissing(connection, "branding_templates", "results_enabled", "INTEGER NOT NULL DEFAULT 1");
    addColumnIfMissing(connection, "branding_templates", "results_height_percent", "REAL NOT NULL DEFAULT 24");
    addColumnIfMissing(connection, "branding_templates", "results_title", "TEXT NOT NULL DEFAULT 'Race results'");
    addColumnIfMissing(connection, "branding_templates", "results_rows_text", "TEXT NOT NULL DEFAULT ''");
    addColumnIfMissing(connection, "branding_templates", "results_background_color", "TEXT NOT NULL DEFAULT '#203241'");
    addColumnIfMissing(connection, "branding_templates", "results_header_color", "TEXT NOT NULL DEFAULT '#172633'");
    addColumnIfMissing(connection, "branding_templates", "results_accent_color", "TEXT NOT NULL DEFAULT '#F0F500'");
  }

  private static void addColumnIfMissing(
      Connection connection,
      String tableName,
      String columnName,
      String definition
  ) throws SQLException {
    if (columnExists(connection, tableName, columnName)) {
      return;
    }
    try (Statement statement = connection.createStatement()) {
      statement.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition);
    }
  }

  private static boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement("PRAGMA table_info(" + tableName + ")");
         ResultSet resultSet = statement.executeQuery()) {
      while (resultSet.next()) {
        if (columnName.equalsIgnoreCase(resultSet.getString("name"))) {
          return true;
        }
      }
    }
    return false;
  }
}
