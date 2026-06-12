package sk.rigo.photofinish.update;

import java.util.ArrayList;
import java.util.List;

public record SemVersion(List<Integer> parts, String original) implements Comparable<SemVersion> {

  public static SemVersion parse(String version) {
    String normalized = version == null ? "0" : version.trim();
    String core = normalized.split("[+-]", 2)[0];
    String[] pieces = core.split("\\.");
    List<Integer> parsed = new ArrayList<>();
    for (String piece : pieces) {
      String digits = piece.replaceAll("[^0-9].*$", "");
      parsed.add(digits.isBlank() ? 0 : Integer.parseInt(digits));
    }
    while (parsed.size() < 3) {
      parsed.add(0);
    }
    return new SemVersion(List.copyOf(parsed), normalized);
  }

  @Override
  public int compareTo(SemVersion other) {
    int max = Math.max(parts.size(), other.parts.size());
    for (int i = 0; i < max; i++) {
      int left = i < parts.size() ? parts.get(i) : 0;
      int right = i < other.parts.size() ? other.parts.get(i) : 0;
      int compared = Integer.compare(left, right);
      if (compared != 0) {
        return compared;
      }
    }
    return 0;
  }
}
