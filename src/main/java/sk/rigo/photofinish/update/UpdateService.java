package sk.rigo.photofinish.update;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdateService {

  private static final Pattern JSON_FIELD = Pattern.compile("\"([A-Za-z0-9_]+)\"\\s*:\\s*\"([^\"]*)\"");

  private final HttpClient httpClient = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(15))
      .followRedirects(HttpClient.Redirect.NORMAL)
      .build();

  public UpdateInfo check(String latestJsonUrl, String currentVersion) throws IOException, InterruptedException {
    if (latestJsonUrl == null || latestJsonUrl.isBlank()) {
      throw new IOException("Latest JSON URL is not configured");
    }

    HttpRequest request = HttpRequest.newBuilder(URI.create(latestJsonUrl))
        .timeout(Duration.ofSeconds(30))
        .GET()
        .build();
    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      if (response.statusCode() == 404) {
        throw new IOException("No latest.json was found at the configured update URL. Check Folder settings.");
      }
      throw new IOException("Update check failed with HTTP " + response.statusCode() + ". Check Folder settings.");
    }

    String body = response.body();
    String latestVersion = field(body, "version");
    if (latestVersion.isBlank()) {
      throw new IOException("latest.json does not contain a version field");
    }
    String installerUrl = field(body, "installerUrl");
    if (installerUrl.isBlank()) {
      installerUrl = field(body, "url");
    }
    String sha256 = field(body, "sha256");
    String notes = field(body, "notes");
    boolean available = SemVersion.parse(latestVersion).compareTo(SemVersion.parse(currentVersion)) > 0;
    return new UpdateInfo(latestVersion, installerUrl, sha256, notes, available);
  }

  public Path downloadInstaller(UpdateInfo updateInfo, Path targetDirectory) throws IOException, InterruptedException {
    if (updateInfo.installerUrl() == null || updateInfo.installerUrl().isBlank()) {
      throw new IOException("Installer URL is not available");
    }

    Files.createDirectories(targetDirectory);
    String filename = Path.of(URI.create(updateInfo.installerUrl()).getPath()).getFileName().toString();
    Path target = targetDirectory.resolve(filename.isBlank() ? "photofinish-installer.exe" : filename);

    HttpRequest request = HttpRequest.newBuilder(URI.create(updateInfo.installerUrl()))
        .timeout(Duration.ofMinutes(5))
        .GET()
        .build();
    HttpResponse<Path> response = httpClient.send(request, HttpResponse.BodyHandlers.ofFile(target));
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throw new IOException("Installer download failed with HTTP " + response.statusCode());
    }

    if (updateInfo.sha256() != null && !updateInfo.sha256().isBlank()) {
      String actual = sha256(target);
      if (!actual.equalsIgnoreCase(updateInfo.sha256())) {
        Files.deleteIfExists(target);
        throw new IOException("Installer SHA-256 mismatch");
      }
    }
    return target;
  }

  private static String field(String json, String name) {
    Matcher matcher = JSON_FIELD.matcher(json);
    while (matcher.find()) {
      if (matcher.group(1).equals(name)) {
        return matcher.group(2);
      }
    }
    return "";
  }

  private static String sha256(Path path) throws IOException {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      try (DigestInputStream inputStream = new DigestInputStream(Files.newInputStream(path), digest)) {
        inputStream.transferTo(OutputStream.nullOutputStream());
      }
      return HexFormat.of().formatHex(digest.digest());
    } catch (NoSuchAlgorithmException ex) {
      throw new IOException("SHA-256 is not available", ex);
    }
  }
}
