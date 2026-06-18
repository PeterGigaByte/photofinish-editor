package sk.rigo.photofinish.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import sk.rigo.photofinish.config.AppSettings;
import sk.rigo.photofinish.model.BrandingMetadata;

class AthleticOfficeServiceTest {

  @Test
  void formatsApiMetadataForRendering() throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/api/races/active", exchange -> json(exchange, """
        {"id":339,"title":"Meeting","openDate":"2026-06-14T00:00:00","venue":"Nitra"}
        """));
    server.createContext("/api/disciplines", exchange -> json(exchange, """
        {"data":[{"id":408,"cameraId":23,"wind":1.23,"start":"2026-06-14T09:15:00",
        "categoryClassShortDescription":"M","disciplineClassShortDescription":"100 m"}]}
        """));
    server.createContext("/api/results/v3", exchange -> json(exchange, """
        {"data":[{"order":1.0,"track":4.0,"bib":0,"firstName":"Adam","lastName":"Runner",
        "bestResult":"15.260","reactionTime":0.1544,"record":"PB"}]}
        """));
    server.start();
    try {
      AppSettings settings = new AppSettings();
      settings.setAthleticOfficeApiEnabled(true);
      settings.setAthleticOfficeBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());

      BrandingMetadata metadata = new AthleticOfficeService().metadataFor(Path.of("23.jpg"), settings);

      assertEquals("+1.2 m/s", metadata.placeholders().get("wind"));
      assertTrue(metadata.headerSubtitle().contains("Vietor: +1.2 m/s"));
      assertEquals("1|4|Adam Runner||15.26||0.154|PB", metadata.resultsRowsText());
    } finally {
      server.stop(0);
    }
  }

  private static void json(com.sun.net.httpserver.HttpExchange exchange, String body) throws java.io.IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
    exchange.sendResponseHeaders(200, bytes.length);
    try (var output = exchange.getResponseBody()) {
      output.write(bytes);
    }
  }
}
