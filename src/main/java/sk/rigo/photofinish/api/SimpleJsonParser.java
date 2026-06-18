package sk.rigo.photofinish.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class SimpleJsonParser {

  private final String json;
  private int index;

  private SimpleJsonParser(String json) {
    this.json = json == null ? "" : json;
  }

  static Object parse(String json) throws IOException {
    SimpleJsonParser parser = new SimpleJsonParser(json);
    Object value = parser.parseValue();
    parser.skipWhitespace();
    if (!parser.isEnd()) {
      throw parser.error("Unexpected trailing JSON content");
    }
    return value;
  }

  private Object parseValue() throws IOException {
    skipWhitespace();
    if (isEnd()) {
      throw error("Unexpected end of JSON");
    }

    char current = json.charAt(index);
    return switch (current) {
      case '{' -> parseObject();
      case '[' -> parseArray();
      case '"' -> parseString();
      case 't' -> parseLiteral("true", Boolean.TRUE);
      case 'f' -> parseLiteral("false", Boolean.FALSE);
      case 'n' -> parseLiteral("null", null);
      default -> {
        if (current == '-' || Character.isDigit(current)) {
          yield parseNumber();
        }
        throw error("Unexpected JSON value");
      }
    };
  }

  private Map<String, Object> parseObject() throws IOException {
    expect('{');
    Map<String, Object> object = new LinkedHashMap<>();
    skipWhitespace();
    if (peek('}')) {
      index++;
      return object;
    }

    while (true) {
      skipWhitespace();
      String key = parseString();
      skipWhitespace();
      expect(':');
      object.put(key, parseValue());
      skipWhitespace();
      if (peek('}')) {
        index++;
        return object;
      }
      expect(',');
    }
  }

  private List<Object> parseArray() throws IOException {
    expect('[');
    List<Object> array = new ArrayList<>();
    skipWhitespace();
    if (peek(']')) {
      index++;
      return array;
    }

    while (true) {
      array.add(parseValue());
      skipWhitespace();
      if (peek(']')) {
        index++;
        return array;
      }
      expect(',');
    }
  }

  private String parseString() throws IOException {
    expect('"');
    StringBuilder builder = new StringBuilder();
    while (!isEnd()) {
      char current = json.charAt(index++);
      if (current == '"') {
        return builder.toString();
      }
      if (current != '\\') {
        builder.append(current);
        continue;
      }
      if (isEnd()) {
        throw error("Unterminated JSON escape");
      }
      char escaped = json.charAt(index++);
      switch (escaped) {
        case '"', '\\', '/' -> builder.append(escaped);
        case 'b' -> builder.append('\b');
        case 'f' -> builder.append('\f');
        case 'n' -> builder.append('\n');
        case 'r' -> builder.append('\r');
        case 't' -> builder.append('\t');
        case 'u' -> builder.append(parseUnicodeEscape());
        default -> throw error("Unsupported JSON escape");
      }
    }
    throw error("Unterminated JSON string");
  }

  private char parseUnicodeEscape() throws IOException {
    if (index + 4 > json.length()) {
      throw error("Invalid JSON unicode escape");
    }
    String hex = json.substring(index, index + 4);
    index += 4;
    try {
      return (char) Integer.parseInt(hex, 16);
    } catch (NumberFormatException ex) {
      throw error("Invalid JSON unicode escape");
    }
  }

  private Object parseLiteral(String literal, Object value) throws IOException {
    if (!json.startsWith(literal, index)) {
      throw error("Invalid JSON literal");
    }
    index += literal.length();
    return value;
  }

  private String parseNumber() throws IOException {
    int start = index;
    if (peek('-')) {
      index++;
    }
    readDigits();
    if (peek('.')) {
      index++;
      readDigits();
    }
    if (peek('e') || peek('E')) {
      index++;
      if (peek('+') || peek('-')) {
        index++;
      }
      readDigits();
    }
    return json.substring(start, index);
  }

  private void readDigits() throws IOException {
    int start = index;
    while (!isEnd() && Character.isDigit(json.charAt(index))) {
      index++;
    }
    if (start == index) {
      throw error("Expected JSON digit");
    }
  }

  private void expect(char expected) throws IOException {
    if (isEnd() || json.charAt(index) != expected) {
      throw error("Expected '" + expected + "'");
    }
    index++;
  }

  private boolean peek(char expected) {
    return !isEnd() && json.charAt(index) == expected;
  }

  private void skipWhitespace() {
    while (!isEnd() && Character.isWhitespace(json.charAt(index))) {
      index++;
    }
  }

  private boolean isEnd() {
    return index >= json.length();
  }

  private IOException error(String message) {
    return new IOException(message + " at character " + index);
  }
}
