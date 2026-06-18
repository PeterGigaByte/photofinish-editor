package sk.rigo.photofinish.image;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class TextTemplateEngine {

  private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
  private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  public String render(String template, Path sourcePath) {
    return render(template, sourcePath, Map.of());
  }

  public String render(String template, Path sourcePath, Map<String, String> placeholders) {
    placeholders = placeholders == null ? Map.of() : placeholders;
    LocalDateTime now = LocalDateTime.now();
    String filename = sourcePath.getFileName() == null ? sourcePath.toString() : sourcePath.getFileName().toString();
    String basename = filename;
    int dot = filename.lastIndexOf('.');
    if (dot > 0) {
      basename = filename.substring(0, dot);
    }

    String rendered = nullToEmpty(template)
        .replace("{filename}", filename)
        .replace("{basename}", basename)
        .replace("{date}", DATE.format(now))
        .replace("{time}", TIME.format(now))
        .replace("{timestamp}", TIMESTAMP.format(now));
    for (Map.Entry<String, String> placeholder : placeholders.entrySet()) {
      rendered = rendered.replace("{" + placeholder.getKey() + "}", nullToEmpty(placeholder.getValue()));
    }
    return rendered;
  }

  private static String nullToEmpty(String value) {
    return value == null ? "" : value;
  }
}
