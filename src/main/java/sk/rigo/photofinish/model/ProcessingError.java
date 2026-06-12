package sk.rigo.photofinish.model;

import java.time.Instant;

public record ProcessingError(
    long id,
    String sourcePath,
    String stage,
    String message,
    String stackTrace,
    Instant createdAt
) {
}
