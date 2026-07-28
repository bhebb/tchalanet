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

  /**
   * Five groups used to have no {@code path} of their own — their header only opened a
   * panel/accordion, never a direct link. This is what lets the console's row-navigates /
   * chevron-expands split (`web-console-sidenav-simplification-v1`) do anything for them: without a
   * {@code path} here, there is nothing for the row's label to link to.
   */
  @Test
  @SuppressWarnings("unchecked")
  void fiveGroupsGainedTheirOwnDestination() {
    var resolver =
        new PrivateShellNavigationResolver(
            new PageModelJsonFragmentRegistry(), new JsonUtils(JsonMapper.builder().build()));

    var navigationDrawer = resolver.resolve(PrivateBootstrapSpace.ADMIN);
    var sections = (List<Map<String, Object>>) navigationDrawer.get("sections");
    var operations = (List<Map<String, Object>>) sections.get(0).get("items");
    var configuration = (List<Map<String, Object>>) sections.get(1).get("items");
    var allItems =
        java.util.stream.Stream.concat(operations.stream(), configuration.stream())
            .collect(java.util.stream.Collectors.toMap(item -> item.get("id"), item -> item));

    assertThat(allItems.get("sellers").get("path")).isEqualTo("/app/admin/seller-terminals");
    assertThat(allItems.get("draws").get("path")).isEqualTo("/app/admin/draws");
    assertThat(allItems.get("reports").get("path")).isEqualTo("/app/admin/reports/daily");
    assertThat(allItems.get("tickets").get("path")).isEqualTo("/app/admin/tickets");
    assertThat(allItems.get("company").get("path")).isEqualTo("/app/admin/business-profile");

    assertThat(childIds(allItems.get("reports")))
        .as("reports-overview dropped from the menu — its route stays, just unlinked")
        .doesNotContain("reports-overview")
        .contains("reports-daily");
    assertThat(childIds(allItems.get("tickets")))
        .as("tickets-overview dropped from the menu — its route stays, just unlinked")
        .doesNotContain("tickets-overview");
  }

  /**
   * Same gap as tenant-admin's five groups, on the platform side: {@code tenants} and {@code
   * operations} already had a matching exact-match child but no {@code path} of their own — ported
   * here from {@code web-console-sidenav-simplification-v1} so the row-navigates / chevron-expands
   * split works on platform too. {@code audit} and {@code archives} gain a destination for the same
   * reason. {@code dashboard}, {@code access}, {@code references}, {@code support-and-content} and
   * {@code tchala} are left alone — none has a single obvious default child.
   */
  @Test
  @SuppressWarnings("unchecked")
  void platformGroupsWithASingleLandingPageGainedTheirOwnDestination() {
    var resolver =
        new PrivateShellNavigationResolver(
            new PageModelJsonFragmentRegistry(), new JsonUtils(JsonMapper.builder().build()));

    var navigationDrawer = resolver.resolve(PrivateBootstrapSpace.PLATFORM);
    var sections = (List<Map<String, Object>>) navigationDrawer.get("sections");
    var allItems =
        ((List<Map<String, Object>>) sections.get(0).get("items"))
            .stream()
                .collect(java.util.stream.Collectors.toMap(item -> item.get("id"), item -> item));

    assertThat(allItems.get("tenants").get("path")).isEqualTo("/app/platform/tenants");
    assertThat(allItems.get("operations").get("path")).isEqualTo("/app/platform/ops");
    assertThat(allItems.get("audit").get("path")).isEqualTo("/app/platform/audit");
    assertThat(allItems.get("archives").get("path")).isEqualTo("/app/platform/archives");

    for (var id : List.of("dashboard", "access", "references", "support-and-content", "tchala")) {
      assertThat(allItems.get(id).get("path"))
          .as("%s: no single obvious default child", id)
          .isNull();
    }
  }

  @SuppressWarnings("unchecked")
  private static List<Object> itemIds(Map<String, Object> section) {
    return ((List<Map<String, Object>>) section.get("items"))
        .stream().map(item -> item.get("id")).map(Object.class::cast).toList();
  }

  @SuppressWarnings("unchecked")
  private static List<Object> childIds(Map<String, Object> group) {
    return ((List<Map<String, Object>>) group.get("children"))
        .stream().map(item -> item.get("id")).map(Object.class::cast).toList();
  }
}
