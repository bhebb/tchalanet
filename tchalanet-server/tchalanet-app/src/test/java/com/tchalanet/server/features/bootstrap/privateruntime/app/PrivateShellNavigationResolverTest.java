package com.tchalanet.server.features.bootstrap.privateruntime.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.features.bootstrap.privateruntime.model.PrivateBootstrapSpace;
import com.tchalanet.server.features.pagemodel.dynamic.providers.json.PageModelJsonFragmentRegistry;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class PrivateShellNavigationResolverTest {

  @Test
  void cashierDoesNotRequireAWebNavigationDrawerFragment() {
    var resolver =
        new PrivateShellNavigationResolver(
            mock(PageModelJsonFragmentRegistry.class), mock(JsonUtils.class));

    assertThat(resolver.resolve(PrivateBootstrapSpace.CASHIER)).isEmpty();
  }

  /**
   * Loads the real {@code private_shell_tenantadmin.json} fragment through the real registry and a
   * real Jackson mapper — no mock — so a malformed edit to that resource (bad JSON, a
   * misplaced/duplicated item, a section that silently disappears) fails here instead of only being
   * noticed once the frontend renders an empty or duplicated menu.
   *
   * <p>Nothing exercised this fragment end-to-end before: the only other test for this resolver
   * mocked both collaborators away.
   */
  @Test
  @SuppressWarnings("unchecked")
  void loadsTheRealTenantAdminNavigationSplitIntoOperationsAndConfiguration() {
    var resolver =
        new PrivateShellNavigationResolver(
            new PageModelJsonFragmentRegistry(), new JsonUtils(JsonMapper.builder().build()));

    var navigationDrawer = resolver.resolve(PrivateBootstrapSpace.ADMIN);
    var sections = (List<Map<String, Object>>) navigationDrawer.get("sections");
    var secondary = (List<Map<String, Object>>) navigationDrawer.get("secondary");

    assertThat(sections).hasSize(2);
    assertThat(itemIds(sections.get(0)))
        .as("operations: what a tenant admin opens every day")
        .containsExactly("dashboard", "sellers", "draws", "reports", "tickets");
    assertThat(itemIds(sections.get(1)))
        .as("configuration: rarely-touched setup, kept out of the daily section")
        .containsExactly("setup", "maryaj-gratis", "games", "limits", "company");
    assertThat(secondary.stream().map(item -> item.get("id")))
        .as("help lives in the drawer footer, not mixed into either section")
        .containsExactly("help");
  }

  @SuppressWarnings("unchecked")
  private static List<Object> itemIds(Map<String, Object> section) {
    return ((List<Map<String, Object>>) section.get("items"))
        .stream().map(item -> item.get("id")).map(Object.class::cast).toList();
  }
}
