package com.tchalanet.server.common.web.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class ErrorContractFixturesTest {

  private static final JsonMapper JSON = JsonMapper.builder().build();

  @Test
  void problemDetailFixturesExposeStableRedactedContract() throws IOException {
    for (Path fixture : jsonFiles(fixturesRoot().resolve("problem-details"))) {
      JsonNode problem = JSON.readTree(Files.readString(fixture));

      assertThat(problem.path("type").asText()).isEqualTo("about:blank");
      assertThat(problem.path("status").asInt()).isBetween(400, 599);
      assertThat(problem.path("detail").asText()).isEqualTo("Request could not be completed");
      assertThat(ErrorDescriptor.isValidCode(problem.path("code").asText()))
          .as("fixture %s has a stable dotted code", fixture)
          .isTrue();
      assertThat(problem.path("category").asText()).isNotBlank();
      assertThat(problem.path("retryPolicy").asText()).isNotBlank();
      assertThat(problem.path("retryable").isBoolean()).isTrue();
      assertThat(problem.path("timestamp").asText()).isNotBlank();
      assertThat(problem.path("method").asText()).isNotBlank();
      assertThat(problem.path("path").asText()).startsWith("/");
      assertThat(problem.path("requestId").asText()).isNotBlank();
      assertThat(problem.path("errorId").asText()).isNotBlank();

      assertThat(problem.has("errors")).isFalse();
      assertThat(problem.has("cause")).isFalse();
      assertThat(problem.has("stack")).isFalse();
      assertThat(problem.toString()).doesNotContain("Exception").doesNotContain("secret");

      if (problem.has("violations")) {
        assertThat(problem.path("violations").isArray()).isTrue();
        for (JsonNode violation : problem.path("violations")) {
          assertThat(ErrorDescriptor.isValidCode(violation.path("code").asText())).isTrue();
          assertThat(violation.path("field").asText()).isNotBlank();
          assertThat(violation.path("target").asText()).isNotBlank();
          assertThat(violation.has("message")).isFalse();
        }
      }
    }
  }

  @Test
  void apiResponseFixturesRetainNoticesAndServiceHealthWithoutServerCopy() throws IOException {
    JsonNode warning = fixture("api-responses/success-with-warning.json");
    assertThat(warning.path("status").asText()).isEqualTo("SUCCESS_WITH_WARNINGS");
    assertThat(warning.path("data").isObject()).isTrue();
    assertThat(warning.path("notices").isArray()).isTrue();
    assertNoticeContract(warning.path("notices").get(0));
    assertThat(warning.path("services").isArray()).isTrue();
    assertThat(warning.path("services")).isEmpty();

    JsonNode partial = fixture("api-responses/partial-section-degradation.json");
    assertThat(partial.path("status").asText()).isEqualTo("PARTIAL");
    assertThat(partial.path("data").isObject()).isTrue();
    assertNoticeContract(partial.path("notices").get(0));
    assertThat(partial.path("notices").get(0).path("kind").asText()).isEqualTo("DEGRADATION");
    assertThat(partial.path("services")).hasSize(1);
    assertThat(partial.path("services").get(0).path("status").asText()).isEqualTo("DEGRADED");
    assertThat(partial.path("services").get(0).path("message").isNull()).isTrue();
  }

  private static void assertNoticeContract(JsonNode notice) {
    assertThat(ErrorDescriptor.isValidCode(notice.path("code").asText())).isTrue();
    assertThat(notice.path("message").isNull()).isTrue();
    assertThat(notice.path("domain").asText()).isNotBlank();
    assertThat(notice.path("severity").asText()).isIn("INFO", "WARN", "ERROR");
    assertThat(notice.path("kind").asText()).isNotBlank();
    assertThat(notice.path("source").path("source").asText()).isNotBlank();
    assertThat(notice.path("target").asText()).isNotBlank();
    assertThat(notice.path("params").isObject()).isTrue();
    assertThat(notice.path("trace").path("requestId").asText()).isNotBlank();
    assertThat(notice.path("trace").path("errorId").asText()).isNotBlank();
    assertThat(notice.toString()).doesNotContain("Exception").doesNotContain("secret");
  }

  private static JsonNode fixture(String relativePath) throws IOException {
    return JSON.readTree(Files.readString(fixturesRoot().resolve(relativePath)));
  }

  private static List<Path> jsonFiles(Path root) throws IOException {
    try (var paths = Files.walk(root)) {
      return paths
          .filter(Files::isRegularFile)
          .filter(path -> path.toString().endsWith(".json"))
          .toList();
    }
  }

  private static Path fixturesRoot() {
    Path serverRoot = serverRoot();
    return serverRoot.resolve("testing/contracts/error-contract/v1");
  }

  private static Path serverRoot() {
    Path current = Path.of("").toAbsolutePath();
    for (Path candidate = current; candidate != null; candidate = candidate.getParent()) {
      if (Files.isDirectory(candidate.resolve("tchalanet-common/src/main/java"))
          && Files.isDirectory(candidate.resolve("testing"))) {
        return candidate;
      }
    }
    throw new IllegalStateException("Cannot locate tchalanet-server root from " + current);
  }
}
