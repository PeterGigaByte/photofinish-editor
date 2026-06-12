package sk.rigo.photofinish.repository;

import sk.rigo.photofinish.db.Database;
import sk.rigo.photofinish.model.ProcessedFile;
import sk.rigo.photofinish.model.ProcessingStatus;
import sk.rigo.photofinish.model.ProcessingSummary;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProcessedFileRepository {

  private final Database database;

  public ProcessedFileRepository(Database database) {
    this.database = database;
  }

  public synchronized Optional<Long> insertQueuedIfAbsent(Path sourcePath) throws SQLException, IOException {
    if (findBySourcePath(sourcePath.toAbsolutePath().toString()).isPresent()) {
      return Optional.empty();
    }

    try (Connection connection = database.getConnection();
         PreparedStatement statement = connection.prepareStatement("""
             INSERT INTO processed_files (
               source_path, source_size, source_last_modified, status, created_at
             )
             VALUES (?, ?, ?, ?, ?)
             """, Statement.RETURN_GENERATED_KEYS)) {
      statement.setString(1, sourcePath.toAbsolutePath().toString());
      statement.setLong(2, Files.size(sourcePath));
      statement.setString(3, Files.getLastModifiedTime(sourcePath).toInstant().toString());
      statement.setString(4, ProcessingStatus.QUEUED.name());
      statement.setString(5, Instant.now().toString());
      statement.executeUpdate();
      try (ResultSet keys = statement.getGeneratedKeys()) {
        if (keys.next()) {
          return Optional.of(keys.getLong(1));
        }
      }
    }
    return Optional.empty();
  }

  public synchronized Optional<ProcessedFile> findById(long id) throws SQLException {
    try (Connection connection = database.getConnection();
         PreparedStatement statement = connection.prepareStatement("SELECT * FROM processed_files WHERE id = ?")) {
      statement.setLong(1, id);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          return Optional.of(map(resultSet));
        }
      }
    }
    return Optional.empty();
  }

  public synchronized Optional<ProcessedFile> findBySourcePath(String sourcePath) throws SQLException {
    try (Connection connection = database.getConnection();
         PreparedStatement statement = connection.prepareStatement("SELECT * FROM processed_files WHERE source_path = ?")) {
      statement.setString(1, sourcePath);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          return Optional.of(map(resultSet));
        }
      }
    }
    return Optional.empty();
  }

  public synchronized void markProcessing(long id, long templateId) throws SQLException {
    try (Connection connection = database.getConnection();
         PreparedStatement statement = connection.prepareStatement("""
             UPDATE processed_files
             SET status = ?, template_id = ?, message = ?
             WHERE id = ?
             """)) {
      statement.setString(1, ProcessingStatus.PROCESSING.name());
      statement.setLong(2, templateId);
      statement.setString(3, "Processing");
      statement.setLong(4, id);
      statement.executeUpdate();
    }
  }

  public synchronized void markExported(long id, Path outputPath, Path stagedPath, String message) throws SQLException {
    markFinished(id, ProcessingStatus.EXPORTED, outputPath, stagedPath, message);
  }

  public synchronized void markPendingExport(long id, Path outputPath, Path stagedPath, String message) throws SQLException {
    markFinished(id, ProcessingStatus.PENDING_EXPORT, outputPath, stagedPath, message);
  }

  public synchronized void markFailed(long id, String message) throws SQLException {
    try (Connection connection = database.getConnection();
         PreparedStatement statement = connection.prepareStatement("""
             UPDATE processed_files
             SET status = ?, message = ?, processed_at = ?
             WHERE id = ?
             """)) {
      statement.setString(1, ProcessingStatus.FAILED.name());
      statement.setString(2, message);
      statement.setString(3, Instant.now().toString());
      statement.setLong(4, id);
      statement.executeUpdate();
    }
  }

  public synchronized List<ProcessedFile> listRecent(int limit) throws SQLException {
    List<ProcessedFile> files = new ArrayList<>();
    try (Connection connection = database.getConnection();
         PreparedStatement statement = connection.prepareStatement("""
             SELECT * FROM processed_files
             ORDER BY id DESC
             LIMIT ?
             """)) {
      statement.setInt(1, limit);
      try (ResultSet resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          files.add(map(resultSet));
        }
      }
    }
    return files;
  }

  public synchronized ProcessingSummary summary() throws SQLException {
    long total = 0;
    long exported = 0;
    long pending = 0;
    long failed = 0;

    try (Connection connection = database.getConnection();
         PreparedStatement statement = connection.prepareStatement("""
             SELECT status, COUNT(*) AS count
             FROM processed_files
             GROUP BY status
             """);
         ResultSet resultSet = statement.executeQuery()) {
      while (resultSet.next()) {
        long count = resultSet.getLong("count");
        total += count;
        ProcessingStatus status = ProcessingStatus.valueOf(resultSet.getString("status"));
        if (status == ProcessingStatus.EXPORTED) {
          exported = count;
        } else if (status == ProcessingStatus.PENDING_EXPORT) {
          pending = count;
        } else if (status == ProcessingStatus.FAILED) {
          failed = count;
        }
      }
    }
    return new ProcessingSummary(total, exported, pending, failed);
  }

  private void markFinished(long id, ProcessingStatus status, Path outputPath, Path stagedPath, String message) throws SQLException {
    try (Connection connection = database.getConnection();
         PreparedStatement statement = connection.prepareStatement("""
             UPDATE processed_files
             SET status = ?, output_path = ?, staged_path = ?, message = ?, processed_at = ?
             WHERE id = ?
             """)) {
      statement.setString(1, status.name());
      statement.setString(2, outputPath == null ? null : outputPath.toString());
      statement.setString(3, stagedPath == null ? null : stagedPath.toString());
      statement.setString(4, message);
      statement.setString(5, Instant.now().toString());
      statement.setLong(6, id);
      statement.executeUpdate();
    }
  }

  private static ProcessedFile map(ResultSet resultSet) throws SQLException {
    return new ProcessedFile(
        resultSet.getLong("id"),
        resultSet.getString("source_path"),
        resultSet.getLong("source_size"),
        parseInstant(resultSet.getString("source_last_modified")),
        resultSet.getString("output_path"),
        resultSet.getString("staged_path"),
        ProcessingStatus.valueOf(resultSet.getString("status")),
        resultSet.getString("message"),
        resultSet.getLong("template_id"),
        parseInstant(resultSet.getString("created_at")),
        parseInstant(resultSet.getString("processed_at"))
    );
  }

  private static Instant parseInstant(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return Instant.parse(value);
  }
}
