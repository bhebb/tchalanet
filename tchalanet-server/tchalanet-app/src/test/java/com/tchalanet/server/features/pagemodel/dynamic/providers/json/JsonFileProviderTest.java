package com.tchalanet.server.features.pagemodel.dynamic.providers.json;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tchalanet.server.common.util.JsonUtils;
import com.tchalanet.server.core.pagemodel.domain.model.PageModelDoc;
import com.tchalanet.server.features.pagemodel.dynamic.PageModelDynamicProviderException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class JsonFileProviderTest {

  private final JsonFileProvider provider =
      new JsonFileProvider(
          new PageModelJsonFragmentRegistry(),
          new JsonUtils(JsonMapper.builder().build()));

  @Test
  void loadsKnownFragment() {
    Object payload =
        provider.load(
            null,
            "shell.sidenav",
            widget("private_sidebar_cashier"),
            "fr",
            null);

    assertThat(payload).isInstanceOf(JsonNode.class);
    assertThat(((JsonNode) payload).has("primary")).isTrue();
  }

  @Test
  void cachesLoadedFragment() {
    Object first = provider.load(null, "shell.sidenav", widget("private_sidebar_cashier"), "fr", null);
    Object second = provider.load(null, "shell.sidenav", widget("private_sidebar_cashier"), "fr", null);

    assertThat(second).isSameAs(first);
  }

  @Test
  void rejectsMissingFileKey() {
    assertThatThrownBy(
            () -> provider.load(null, "shell.header", widget(null), "fr", null))
        .isInstanceOf(PageModelDynamicProviderException.class)
        .hasMessageContaining("props.file_key is required");
  }

  private static PageModelDoc.WidgetConfig widget(String fileKey) {
    Map<String, Object> props = fileKey == null ? Map.of() : Map.of("file_key", fileKey);
    return new PageModelDoc.WidgetConfig(
        "JsonFragment",
        new PageModelDoc.WidgetBinding("dynamic", JsonFileProvider.SOURCE),
        props);
  }
}
