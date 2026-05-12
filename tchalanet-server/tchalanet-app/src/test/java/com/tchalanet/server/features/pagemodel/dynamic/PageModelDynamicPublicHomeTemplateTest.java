package com.tchalanet.server.features.pagemodel.dynamic;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.util.JsonUtils;
import com.tchalanet.server.core.pagemodel.domain.model.PageModelDoc;
import com.tchalanet.server.features.pagemodel.dynamic.providers.HeroProvider;
import com.tchalanet.server.features.pagemodel.dynamic.providers.PlansProvider;
import com.tchalanet.server.features.pagemodel.dynamic.providers.PublicDrawResultsProvider;
import com.tchalanet.server.features.pagemodel.dynamic.providers.PublicFeaturesProvider;
import com.tchalanet.server.features.pagemodel.dynamic.providers.PublicNewsProvider;
import com.tchalanet.server.features.pagemodel.dynamic.providers.PublicTchalaProvider;
import com.tchalanet.server.features.pagemodel.dynamic.providers.PublicTestimonialsProvider;
import com.tchalanet.server.features.pagemodel.dynamic.providers.json.JsonFileProvider;
import com.tchalanet.server.features.pagemodel.dynamic.providers.json.PageModelJsonFragmentRegistry;
import java.io.InputStream;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class PageModelDynamicPublicHomeTemplateTest {

  private final JsonUtils jsonUtils = new JsonUtils(JsonMapper.builder().build());

  @Test
  void publicHomeTemplateHasProviderForConfiguredDynamicSources() throws Exception {
    var resolver =
        new PageModelDynamicResolver(
            List.of(
                new JsonFileProvider(new PageModelJsonFragmentRegistry(), jsonUtils),
                new HeroProvider(),
                new PublicDrawResultsProvider(null),
                new PublicFeaturesProvider(),
                new PublicNewsProvider(null),
                new PublicTchalaProvider(),
                new PublicTestimonialsProvider(),
                new PlansProvider(null)));

    var payload = resolver.resolve(loadTemplate(), "fr", null);

    assertThat(payload.errors())
        .noneMatch(error -> "NO_PROVIDER".equals(error.code()));
  }

  private PageModelDoc loadTemplate() throws Exception {
    try (InputStream is = getClass().getResourceAsStream("/pagemodel/public.home.json")) {
      JsonNode root = jsonUtils.readValue(is, JsonNode.class);
      return jsonUtils.treeToValue(root.get("model"), PageModelDoc.class);
    }
  }
}
