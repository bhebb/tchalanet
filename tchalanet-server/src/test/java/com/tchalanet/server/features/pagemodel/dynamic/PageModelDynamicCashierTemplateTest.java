package com.tchalanet.server.features.pagemodel.dynamic;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.util.JsonUtils;
import com.tchalanet.server.core.pagemodel.domain.model.PageModelDoc;
import com.tchalanet.server.features.pagemodel.dynamic.providers.CashierLimitsProvider;
import com.tchalanet.server.features.pagemodel.dynamic.providers.CashierNextDrawsProvider;
import com.tchalanet.server.features.pagemodel.dynamic.providers.CashierOverviewProvider;
import com.tchalanet.server.features.pagemodel.dynamic.providers.CashierQuickSaleProvider;
import com.tchalanet.server.features.pagemodel.dynamic.providers.CashierRecentTicketsProvider;
import com.tchalanet.server.features.pagemodel.dynamic.providers.CashierSessionProvider;
import com.tchalanet.server.features.pagemodel.dynamic.providers.json.JsonFileProvider;
import com.tchalanet.server.features.pagemodel.dynamic.providers.json.PageModelJsonFragmentRegistry;
import java.io.InputStream;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class PageModelDynamicCashierTemplateTest {

  private final JsonUtils jsonUtils = new JsonUtils(JsonMapper.builder().build());

  @Test
  void cashierTemplateHasProviderForConfiguredDynamicSources() throws Exception {
    var resolver =
        new PageModelDynamicResolver(
            List.of(
                new JsonFileProvider(new PageModelJsonFragmentRegistry(), jsonUtils),
                new CashierOverviewProvider(null),
                new CashierQuickSaleProvider(),
                new CashierRecentTicketsProvider(),
                new CashierSessionProvider(),
                new CashierNextDrawsProvider(),
                new CashierLimitsProvider()));

    var payload = resolver.resolve(loadTemplate(), "fr", null);

    assertThat(payload.errors())
        .noneMatch(error -> "NO_PROVIDER".equals(error.code()));
  }

  private PageModelDoc loadTemplate() throws Exception {
    try (InputStream is = getClass().getResourceAsStream("/pagemodel/private.dashboard.cashier.json")) {
      JsonNode root = jsonUtils.readValue(is, JsonNode.class);
      return jsonUtils.treeToValue(root.get("model"), PageModelDoc.class);
    }
  }
}
