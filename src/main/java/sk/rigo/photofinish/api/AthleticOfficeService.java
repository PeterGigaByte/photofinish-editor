package sk.rigo.photofinish.api;

import sk.rigo.photofinish.config.AppSettings;
import sk.rigo.photofinish.model.BrandingMetadata;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AthleticOfficeService {

  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(8);
  private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("HH:mm");

  private final HttpClient httpClient;

  public AthleticOfficeService() {
    this(HttpClient.newBuilder()
        .connectTimeout(REQUEST_TIMEOUT)
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build());
  }

  AthleticOfficeService(HttpClient httpClient) {
    this.httpClient = httpClient;
  }

  public BrandingMetadata metadataFor(Path sourcePath, AppSettings settings) throws IOException, InterruptedException {
    if (!settings.isAthleticOfficeApiEnabled()) {
      return BrandingMetadata.empty();
    }

    String baseUrl = normalizedBaseUrl(settings.getAthleticOfficeBaseUrl());
    if (baseUrl.isBlank()) {
      throw new IOException("AthleticOffice API base URL is not configured");
    }

    String cameraId = cameraIdFromFilename(sourcePath);
    Race race = fetchRaceOrFallback(baseUrl, settings);
    String raceId = firstNonBlank(settings.getAthleticOfficeActiveRaceId(), race.id());
    if (raceId.isBlank()) {
      throw new IOException("AthleticOffice active race ID is not available");
    }

    List<Discipline> disciplines = fetchDisciplines(baseUrl, settings, raceId);
    Discipline discipline = findByCameraId(disciplines, cameraId);
    if (discipline == null) {
      throw new IOException("AthleticOffice did not return a discipline with cameraId " + cameraId);
    }

    List<ResultRow> results = fetchResults(baseUrl, settings, raceId, discipline.id());
    return metadata(cameraId, raceId, race, discipline, results);
  }

  public String check(AppSettings settings) throws IOException, InterruptedException {
    String baseUrl = normalizedBaseUrl(settings.getAthleticOfficeBaseUrl());
    if (baseUrl.isBlank()) {
      throw new IOException("AthleticOffice API base URL is not configured");
    }

    Race race = fetchRaceOrFallback(baseUrl, settings);
    String raceId = firstNonBlank(settings.getAthleticOfficeActiveRaceId(), race.id());
    if (raceId.isBlank()) {
      throw new IOException("AthleticOffice active race ID is not available");
    }

    List<Discipline> disciplines = fetchDisciplines(baseUrl, settings, raceId);
    long cameraIds = disciplines.stream()
        .filter(discipline -> !discipline.cameraId().isBlank() || !discipline.uniqueCameraId().isBlank())
        .count();
    String title = firstNonBlank(race.title(), "race " + raceId);
    return "AthleticOffice OK: " + title + ", disciplines " + disciplines.size() + ", camera IDs " + cameraIds + ".";
  }

  private Race fetchRaceOrFallback(String baseUrl, AppSettings settings) throws IOException, InterruptedException {
    String configuredRaceId = normalizeText(settings.getAthleticOfficeActiveRaceId());
    try {
      return fetchActiveRace(baseUrl, settings);
    } catch (IOException ex) {
      if (configuredRaceId.isBlank()) {
        throw ex;
      }
      return new Race(configuredRaceId, "", "", "");
    }
  }

  private Race fetchActiveRace(String baseUrl, AppSettings settings) throws IOException, InterruptedException {
    Map<String, Object> root = fetchObject(baseUrl + "/api/races/active", settings, "");
    return new Race(
        text(root, "id"),
        text(root, "title"),
        text(root, "openDate"),
        text(root, "venue")
    );
  }

  private List<Discipline> fetchDisciplines(String baseUrl, AppSettings settings, String raceId)
      throws IOException, InterruptedException {
    Map<String, Object> root = fetchObject(baseUrl + "/api/disciplines?", settings, raceId);
    List<Discipline> disciplines = new ArrayList<>();
    for (Map<String, Object> item : dataObjects(root)) {
      disciplines.add(new Discipline(
          text(item, "id"),
          text(item, "cameraId"),
          text(item, "uniqueCameraId"),
          text(item, "wind"),
          text(item, "categoryClassDescription"),
          text(item, "categoryClassShortDescription"),
          text(item, "disciplineClassDescription"),
          text(item, "disciplineClassShortDescription"),
          text(item, "phaseClassDescription"),
          text(item, "start"),
          text(item, "participantsCount")
      ));
    }
    return disciplines;
  }

  private List<ResultRow> fetchResults(String baseUrl, AppSettings settings, String raceId, String disciplineId)
      throws IOException, InterruptedException {
    String encodedIds = URLEncoder.encode("[" + disciplineId + "]", StandardCharsets.UTF_8);
    String url = baseUrl + "/api/results/v3?disciplineIds=" + encodedIds + "&onlyUnregistered=false";
    Map<String, Object> root = fetchObject(url, settings, raceId);
    List<ResultRow> rows = new ArrayList<>();
    for (Map<String, Object> item : dataObjects(root)) {
      rows.add(new ResultRow(
          text(item, "order"),
          text(item, "orderHelp"),
          text(item, "track"),
          text(item, "firstName"),
          text(item, "lastName"),
          text(item, "relayTeamLabel"),
          text(item, "clubAbbreviation"),
          text(item, "bib"),
          text(item, "bestResult"),
          text(item, "netResult"),
          text(item, "bestResultAccurate"),
          text(item, "bestWind"),
          text(item, "reactionTime"),
          text(item, "regularTrialWind"),
          text(item, "record")
      ));
    }
    rows.sort(Comparator
        .comparingInt((ResultRow row) -> parseOrder(firstNonBlank(row.order(), row.orderHelp())))
        .thenComparingInt(row -> parseOrder(row.track())));
    return rows;
  }

  private Map<String, Object> fetchObject(String url, AppSettings settings, String raceId)
      throws IOException, InterruptedException {
    HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
        .timeout(REQUEST_TIMEOUT)
        .header("Accept", "application/json")
        .header("User-Agent", "PhotoFinish Branding Studio");

    if (!raceId.isBlank()) {
      builder.header("AthleticOffice-ActiveRace-Id", raceId);
    }
    String connectionId = normalizeText(settings.getAthleticOfficeConnectionId());
    if (!connectionId.isBlank()) {
      builder.header("AthleticOffice-ApplicationConnectionId", connectionId);
    }

    HttpResponse<String> response = httpClient.send(
        builder.GET().build(),
        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
    );
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throw new IOException("AthleticOffice API returned HTTP " + response.statusCode() + " for " + url);
    }

    Object parsed = SimpleJsonParser.parse(response.body());
    if (parsed instanceof Map<?, ?> map) {
      return castMap(map);
    }
    throw new IOException("AthleticOffice API returned a non-object JSON payload for " + url);
  }

  private static BrandingMetadata metadata(
      String cameraId,
      String raceId,
      Race race,
      Discipline discipline,
      List<ResultRow> results
  ) {
    String disciplineName = firstNonBlank(discipline.disciplineShort(), discipline.discipline());
    String categoryName = firstNonBlank(discipline.categoryShort(), discipline.category());
    String resultsTitle = joinNonBlank(" - ", categoryName, disciplineName);
    String wind = formatWind(firstNonBlank(
        discipline.wind(),
        results.stream()
            .map(row -> firstNonBlank(row.bestWind(), row.regularTrialWind()))
            .filter(value -> !value.isBlank())
            .findFirst()
            .orElse("")
    ));
    String headerSubtitle = joinNonBlank(
        " | ",
        race.venue(),
        displayDate(race.openDate()),
        resultsTitle,
        displayTime(discipline.start()),
        wind.isBlank() ? "" : "Vietor: " + wind
    );

    Map<String, String> placeholders = new LinkedHashMap<>();
    put(placeholders, "apiRaceId", raceId);
    put(placeholders, "raceId", raceId);
    put(placeholders, "raceTitle", race.title());
    put(placeholders, "eventTitle", race.title());
    put(placeholders, "raceVenue", race.venue());
    put(placeholders, "venue", race.venue());
    put(placeholders, "raceDate", displayDate(race.openDate()));
    put(placeholders, "cameraId", cameraId);
    put(placeholders, "disciplineId", discipline.id());
    put(placeholders, "discipline", discipline.discipline());
    put(placeholders, "disciplineShort", discipline.disciplineShort());
    put(placeholders, "category", discipline.category());
    put(placeholders, "categoryShort", discipline.categoryShort());
    put(placeholders, "phase", discipline.phase());
    put(placeholders, "wind", wind);
    put(placeholders, "bestWind", wind);
    put(placeholders, "startDate", displayDate(discipline.start()));
    put(placeholders, "startTime", displayTime(discipline.start()));
    put(placeholders, "startDateTime", displayDateTime(discipline.start()));
    put(placeholders, "participantsCount", discipline.participantsCount());
    put(placeholders, "resultsTitle", resultsTitle);
    put(placeholders, "resultsTotal", Integer.toString(results.size()));

    String rowsText = resultRowsText(results);
    return new BrandingMetadata(
        placeholders,
        firstNonBlank(race.title(), resultsTitle),
        headerSubtitle,
        resultsTitle,
        rowsText
    );
  }

  private static String resultRowsText(List<ResultRow> results) {
    List<String> lines = new ArrayList<>();
    for (ResultRow row : results) {
      lines.add(String.join("|",
          cleanCell(formatInteger(firstNonBlank(row.order(), row.orderHelp()))),
          cleanCell(formatInteger(row.track())),
          cleanCell(athleteName(row)),
          cleanCell(formatBib(row.bib())),
          cleanCell(formatResult(firstNonBlank(row.bestResult(), row.netResult(), row.bestResultAccurate()))),
          "",
          cleanCell(formatReaction(row.reactionTime())),
          cleanCell(row.record())
      ));
    }
    return String.join(System.lineSeparator(), lines);
  }

  private static Discipline findByCameraId(List<Discipline> disciplines, String cameraId) {
    String normalized = normalizeIdentifier(cameraId);
    for (Discipline discipline : disciplines) {
      if (normalized.equals(normalizeIdentifier(discipline.cameraId()))) {
        return discipline;
      }
    }
    for (Discipline discipline : disciplines) {
      if (normalized.equals(normalizeIdentifier(discipline.uniqueCameraId()))) {
        return discipline;
      }
    }
    return null;
  }

  private static String cameraIdFromFilename(Path sourcePath) throws IOException {
    String filename = sourcePath.getFileName() == null ? sourcePath.toString() : sourcePath.getFileName().toString();
    int dot = filename.lastIndexOf('.');
    String basename = dot > 0 ? filename.substring(0, dot) : filename;
    String cameraId = normalizeText(basename);
    if (cameraId.isBlank()) {
      throw new IOException("Input filename must contain the AthleticOffice cameraId");
    }
    return cameraId;
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> castMap(Map<?, ?> map) {
    return (Map<String, Object>) map;
  }

  private static List<Map<String, Object>> dataObjects(Map<String, Object> root) {
    Object data = root.get("data");
    if (!(data instanceof List<?> items)) {
      return List.of();
    }
    List<Map<String, Object>> objects = new ArrayList<>();
    for (Object item : items) {
      if (item instanceof Map<?, ?> map) {
        objects.add(castMap(map));
      }
    }
    return objects;
  }

  private static String text(Map<String, Object> object, String key) {
    Object value = object.get(key);
    if (value == null) {
      return "";
    }
    return value.toString().trim();
  }

  private static String normalizedBaseUrl(String value) {
    String normalized = normalizeText(value);
    if (normalized.isBlank()) {
      return "";
    }
    if (!normalized.matches("^[A-Za-z][A-Za-z0-9+.-]*://.*")) {
      normalized = "http://" + normalized;
    }
    while (normalized.endsWith("/")) {
      normalized = normalized.substring(0, normalized.length() - 1);
    }
    if (normalized.endsWith("/api")) {
      normalized = normalized.substring(0, normalized.length() - 4);
    }
    return normalized;
  }

  private static String normalizeIdentifier(String value) {
    String normalized = normalizeText(value);
    if (normalized.isBlank()) {
      return "";
    }
    try {
      return new BigDecimal(normalized).stripTrailingZeros().toPlainString();
    } catch (NumberFormatException ex) {
      return normalized;
    }
  }

  private static int parseOrder(String value) {
    String normalized = normalizeIdentifier(value);
    if (normalized.isBlank()) {
      return Integer.MAX_VALUE;
    }
    try {
      return new BigDecimal(normalized).intValue();
    } catch (NumberFormatException ex) {
      return Integer.MAX_VALUE;
    }
  }

  private static String formatInteger(String value) {
    String normalized = normalizeIdentifier(value);
    if (normalized.isBlank()) {
      return "";
    }
    try {
      return new BigDecimal(normalized).setScale(0, RoundingMode.DOWN).toPlainString();
    } catch (NumberFormatException ex) {
      return normalized;
    }
  }

  private static String formatBib(String value) {
    String bib = formatInteger(value);
    return "0".equals(bib) ? "" : bib;
  }

  private static String formatResult(String value) {
    String normalized = normalizeText(value);
    if (normalized.isBlank() || normalized.contains(":")) {
      return normalized;
    }
    try {
      return new BigDecimal(normalized).stripTrailingZeros().toPlainString();
    } catch (NumberFormatException ex) {
      return normalized;
    }
  }

  private static String formatReaction(String value) {
    String normalized = normalizeText(value);
    if (normalized.isBlank()) {
      return "";
    }
    try {
      return new BigDecimal(normalized).setScale(3, RoundingMode.HALF_UP).toPlainString();
    } catch (NumberFormatException ex) {
      return normalized;
    }
  }

  private static String formatWind(String value) {
    String normalized = normalizeText(value);
    if (normalized.isBlank()) {
      return "";
    }
    if (normalized.toLowerCase().contains("m/s")) {
      return normalized;
    }
    try {
      BigDecimal wind = new BigDecimal(normalized.replace(',', '.')).setScale(1, RoundingMode.HALF_UP);
      String prefix = wind.signum() > 0 ? "+" : "";
      return prefix + wind.toPlainString() + " m/s";
    } catch (NumberFormatException ex) {
      return normalized;
    }
  }

  private static String displayDate(String value) {
    LocalDateTime dateTime = parseDateTime(value);
    if (dateTime == null) {
      return "";
    }
    return DISPLAY_DATE.format(dateTime);
  }

  private static String displayTime(String value) {
    LocalDateTime dateTime = parseDateTime(value);
    if (dateTime == null) {
      return "";
    }
    return DISPLAY_TIME.format(dateTime);
  }

  private static String displayDateTime(String value) {
    String date = displayDate(value);
    String time = displayTime(value);
    return joinNonBlank(" ", date, time);
  }

  private static LocalDateTime parseDateTime(String value) {
    String normalized = normalizeText(value);
    if (normalized.isBlank()) {
      return null;
    }
    try {
      return LocalDateTime.parse(normalized);
    } catch (DateTimeParseException ex) {
      return null;
    }
  }

  private static String athleteName(ResultRow row) {
    if (!row.relayTeamLabel().isBlank()) {
      return row.relayTeamLabel();
    }
    String name = joinNonBlank(" ", row.firstName(), row.lastName());
    if (!name.isBlank()) {
      return name;
    }
    return row.clubAbbreviation();
  }

  private static String cleanCell(String value) {
    return normalizeText(value)
        .replace('|', '/')
        .replace('\r', ' ')
        .replace('\n', ' ');
  }

  private static void put(Map<String, String> values, String key, String value) {
    values.put(key, normalizeText(value));
  }

  private static String firstNonBlank(String... values) {
    for (String value : values) {
      String normalized = normalizeText(value);
      if (!normalized.isBlank()) {
        return normalized;
      }
    }
    return "";
  }

  private static String joinNonBlank(String separator, String... values) {
    List<String> parts = new ArrayList<>();
    for (String value : values) {
      String normalized = normalizeText(value);
      if (!normalized.isBlank()) {
        parts.add(normalized);
      }
    }
    return String.join(separator, parts);
  }

  private static String normalizeText(String value) {
    return value == null ? "" : value.trim();
  }

  private record Race(String id, String title, String openDate, String venue) {
  }

  private record Discipline(
      String id,
      String cameraId,
      String uniqueCameraId,
      String wind,
      String category,
      String categoryShort,
      String discipline,
      String disciplineShort,
      String phase,
      String start,
      String participantsCount
  ) {
  }

  private record ResultRow(
      String order,
      String orderHelp,
      String track,
      String firstName,
      String lastName,
      String relayTeamLabel,
      String clubAbbreviation,
      String bib,
      String bestResult,
      String netResult,
      String bestResultAccurate,
      String bestWind,
      String reactionTime,
      String regularTrialWind,
      String record
  ) {
  }
}
