package sk.rigo.photofinish.image;

import sk.rigo.photofinish.model.ProcessingStatus;

import java.nio.file.Path;

public record ExportResult(ProcessingStatus status, Path outputPath, Path stagedPath, String message) {
}
