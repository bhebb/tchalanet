package com.tchalanet.server.platform.idempotence.internal.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.util.JsonUtils;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class RequestHasherTest {

  private final JsonUtils jsonUtils = new JsonUtils(JsonMapper.builder().build());

  @Test
  void hashesObjectsWithCanonicalKeyOrdering() {
    var first = Map.of("b", 2, "a", Map.of("y", 2, "x", 1));
    var second = Map.of("a", Map.of("x", 1, "y", 2), "b", 2);

    assertThat(RequestHasher.sha256Normalized(jsonUtils, first))
        .isEqualTo(RequestHasher.sha256Normalized(jsonUtils, second));
  }

  @Test
  void preservesArrayOrdering() {
    var first = Map.of("lines", List.of(1, 2));
    var second = Map.of("lines", List.of(2, 1));

    assertThat(RequestHasher.sha256Normalized(jsonUtils, first))
        .isNotEqualTo(RequestHasher.sha256Normalized(jsonUtils, second));
  }
}
