package sk.rigo.photofinish.update;

public record UpdateInfo(
    String version,
    String installerUrl,
    String sha256,
    String notes,
    boolean available
) {
}
