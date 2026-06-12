package sk.rigo.photofinish.db;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
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
            updated_at TEXT NOT NULL
          )
          """);
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
}
