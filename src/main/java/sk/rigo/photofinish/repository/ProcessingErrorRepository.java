package sk.rigo.photofinish.repository;

import sk.rigo.photofinish.db.Database;
import sk.rigo.photofinish.model.ProcessingError;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ProcessingErrorRepository {

  private final Database database;

  public ProcessingErrorRepository(Database database) {
    this.database = database;
  }

  public synchronized void log(String sourcePath, String stage, String message, Throwable throwable) {
    try (Connection connection = database.getConnection();
         PreparedStatement statement = connection.prepareStatement("""
             INSERT INTO processing_errors(source_path, stage, message, stack_trace, created_at)
             VALUES (?, ?, ?, ?, ?)
             """)) {
      statement.setString(1, sourcePath);
      statement.setString(2, stage);
      statement.setString(3, message);
      statement.setString(4, stackTrace(throwable));
      statement.setString(5, Instant.now().toString());
      statement.executeUpdate();
    } catch (SQLException ignored) {
      // Logging must not break the processing thread.
    }
  }

  public synchronized List<ProcessingError> listRecent(int limit) throws SQLException {
    List<ProcessingError> errors = new ArrayList<>();
    try (Connection connection = database.getConnection();
         PreparedStatement statement = connection.prepareStatement("""
             SELECT * FROM processing_errors
             ORDER BY id DESC
             LIMIT ?
             """)) {
      statement.setInt(1, limit);
      try (ResultSet resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          errors.add(map(resultSet));
        }
      }
    }
    return errors;
  }

  private static ProcessingError map(ResultSet resultSet) throws SQLException {
    return new ProcessingError(
        resultSet.getLong("id"),
        resultSet.getString("source_path"),
        resultSet.getString("stage"),
        resultSet.getString("message"),
        resultSet.getString("stack_trace"),
        Instant.parse(resultSet.getString("created_at"))
    );
  }

  private static String stackTrace(Throwable throwable) {
    if (throwable == null) {
      return "";
    }
    StringWriter stringWriter = new StringWriter();
    throwable.printStackTrace(new PrintWriter(stringWriter));
    return stringWriter.toString();
  }
}
