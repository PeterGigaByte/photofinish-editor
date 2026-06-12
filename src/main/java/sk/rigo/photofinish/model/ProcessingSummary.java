package sk.rigo.photofinish.model;

public record ProcessingSummary(long total, long exported, long pendingExport, long failed) {
}
