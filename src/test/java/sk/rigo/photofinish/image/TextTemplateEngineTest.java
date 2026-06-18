package sk.rigo.photofinish.image;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TextTemplateEngineTest {

  @Test
  void rendersBuiltInAndExternalPlaceholders() {
    TextTemplateEngine engine = new TextTemplateEngine();

    String rendered = engine.render(
        "{basename}|{filename}|{cameraId}|{raceTitle}|{discipline}",
        Path.of("23.jpg"),
        Map.of(
            "cameraId", "23",
            "raceTitle", "Meeting",
            "discipline", "100 m"
        )
    );

    assertEquals("23|23.jpg|23|Meeting|100 m", rendered);
  }
}
