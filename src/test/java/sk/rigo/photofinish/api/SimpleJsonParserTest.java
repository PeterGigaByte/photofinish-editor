package sk.rigo.photofinish.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SimpleJsonParserTest {

  @Test
  void parsesNestedApiPayload() throws Exception {
    Object parsed = SimpleJsonParser.parse("""
        {
          "data": [
            {
              "id": 408,
              "cameraId": 23,
              "disciplineClassDescription": "100 m",
              "registrationMembers": [
                {"firstName": "Milan"}
              ]
            }
          ],
          "total": 1,
          "errors": null
        }
        """);

    Map<?, ?> root = assertInstanceOf(Map.class, parsed);
    List<?> data = assertInstanceOf(List.class, root.get("data"));
    Map<?, ?> discipline = assertInstanceOf(Map.class, data.getFirst());
    List<?> members = assertInstanceOf(List.class, discipline.get("registrationMembers"));
    Map<?, ?> member = assertInstanceOf(Map.class, members.getFirst());

    assertEquals("408", discipline.get("id"));
    assertEquals("23", discipline.get("cameraId"));
    assertEquals("100 m", discipline.get("disciplineClassDescription"));
    assertEquals("Milan", member.get("firstName"));
    assertEquals("1", root.get("total"));
    assertNull(root.get("errors"));
  }

  @Test
  void decodesEscapedStrings() throws Exception {
    Map<?, ?> root = assertInstanceOf(Map.class, SimpleJsonParser.parse("""
        {"text":"Line\\n\\u0043asy","enabled":true}
        """));

    assertEquals("Line\nCasy", root.get("text"));
    assertEquals(Boolean.TRUE, root.get("enabled"));
  }
}
