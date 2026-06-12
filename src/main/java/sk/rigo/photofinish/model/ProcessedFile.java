package sk.rigo.photofinish.model;

import java.time.Instant;

public record ProcessedFile(
    long id,
    String sourcePath,
    long sourceSize,
    Instant sourceLastModified,
    String outputPath,
    String stagedPath,
    ProcessingStatus status,
    String message,
    long templateId,
    Instant createdAt,
    Instant processedAt
) {
}
