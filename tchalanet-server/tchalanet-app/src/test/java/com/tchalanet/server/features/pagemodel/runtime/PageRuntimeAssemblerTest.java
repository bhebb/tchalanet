package com.tchalanet.server.features.pagemodel.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.core.pagemodel.api.model.PageModelDoc;
import com.tchalanet.server.features.pagemodel.shared.PageDynamicPayload;
import com.tchalanet.server.features.pagemodel.shared.WidgetDynamicError;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class PageRuntimeAssemblerTest {

  private final JsonUtils jsonUtils = new JsonUtils(JsonMapper.builder().build());
  private final PageRuntimeAssembler assembler = new PageRuntimeAssembler(jsonUtils);

  @Test
  void assemblesResolvedPublicRuntimeWithoutInternalDefinitionFields() {
    var jsonBinding = new PageModelDoc.WidgetBinding("dynamic", "jsonFile");
    var businessBinding = new PageModelDoc.WidgetBinding("dynamic", "public_draw_results");
    var doc =
        new PageModelDoc(
            new PageModelDoc.Meta(
                "public.home", "public", "home", "public_home", 2, List.of("fr"), "fr"),
            new PageModelDoc.Theme("tchalanet", "light", 0, Map.of("ignored", "value")),
            new PageModelDoc.Shell(
                new PageModelDoc.ShellSectionConfig(
                    "PublicHeader", jsonBinding, null, Map.of("fileKey", "public_header_links")),
                null,
                new PageModelDoc.ShellSectionConfig(
                    "PublicFooter", jsonBinding, null, Map.of("fileKey", "public_footer_links")),
                null,
                null,
                null),
            new PageModelDoc.Content(
                new PageModelDoc.Layout(
                    "GridLayout",
                    List.of(
                        new PageModelDoc.LayoutRow(
                            "hero",
                            "layout.hero",
                            List.of(new PageModelDoc.LayoutColumn(12, List.of("home.hero")))))),
                Map.of(
                    "home.hero",
                    new PageModelDoc.WidgetConfig(
                        "HeroWidget", jsonBinding, Map.of("fileKey", "public_hero")),
                    "home.draws",
                    new PageModelDoc.WidgetConfig(
                        "DrawsWidget", businessBinding, Map.of("maxItems", 5)))));

    var dynamic =
        new PageDynamicPayload(
            Map.of(
                "shell.header",
                Map.of("brand", Map.of("id", "brand"), "secondary", List.of()),
                "shell.footer",
                Map.of("description_key", "public.footer.description"),
                "home.hero",
                Map.of("title_key", "home.hero.title"),
                "home.draws",
                Map.of("items", List.of(1, 2))),
            List.of(new WidgetDynamicError("home.draws", "provider", "DEGRADED", "internal")));

    var runtime = assembler.assemble(doc, dynamic);
    String json = jsonUtils.toJson(runtime);

    assertThat(runtime.meta().logicalId()).isEqualTo("public.home");
    assertThat(runtime.meta().schemaVersion()).isEqualTo(2);
    assertThat(runtime.shell()).isInstanceOf(PageRuntimeResponse.PublicShell.class);
    assertThat(runtime.content().widgets().get("home.hero").props())
        .isEqualTo(Map.of("titleKey", "home.hero.title"));
    assertThat(runtime.dynamic().widgets()).containsKey("home.draws").doesNotContainKey("home.hero");
    assertThat(runtime.dynamic().errors().getFirst().code()).isEqualTo("DEGRADED");
    assertThat(json)
        .contains("\"dynamic\"")
        .doesNotContain("\"notices\"")
        .doesNotContain("binding")
        .doesNotContain("fileKey")
        .doesNotContain("provider")
        .doesNotContain("internal")
        .doesNotContain("schema_version")
        .doesNotContain("description_key");
  }

  @Test
  void assemblesPrivateShellWithCamelCaseRouteDestinations() {
    var doc =
        new PageModelDoc(
            new PageModelDoc.Meta(
                "private.dashboard.tenant_admin",
                "private",
                "dashboard",
                "tenant_admin",
                2,
                List.of("fr"),
                "fr"),
            null,
            null,
            new PageModelDoc.Content(new PageModelDoc.Layout("GridLayout", List.of()), Map.of()));
    var dynamic =
        new PageDynamicPayload(
            Map.of(
                "shell.root",
                Map.of(
                    "fragment_type",
                    "PrivateShell",
                    "topAppBar",
                    Map.of("title", Map.of("label_key", "surface.tenant_admin")),
                    "navigationDrawer",
                    Map.of(
                        "topDestinations",
                        List.of(
                            Map.of(
                                "id",
                                "dashboard",
                                "label_key",
                                "nav.dashboard",
                                "kind",
                                "internal",
                                "path",
                                "/app/admin"))))),
            List.of());

    var runtime = assembler.assemble(doc, dynamic);
    String json = jsonUtils.toJson(runtime);

    assertThat(runtime.shell()).isInstanceOf(PageRuntimeResponse.PrivateShell.class);
    assertThat(json)
        .contains("\"labelKey\":\"nav.dashboard\"")
        .contains("\"kind\":\"route\"")
        .contains("\"value\":\"/app/admin\"")
        .doesNotContain("fragment_type")
        .doesNotContain("label_key")
        .doesNotContain("\"path\"")
        .doesNotContain("\"kind\":\"internal\"");
  }

  @Test
  void assemblesGroupedPlatformNavigationChildren() {
    var doc =
        new PageModelDoc(
            new PageModelDoc.Meta(
                "private.dashboard.super_admin",
                "private",
                "dashboard",
                "super_admin",
                2,
                List.of("fr"),
                "fr"),
            null,
            null,
            new PageModelDoc.Content(new PageModelDoc.Layout("GridLayout", List.of()), Map.of()));

    var runtime = assembler.assemble(doc, null);
    String json = jsonUtils.toJson(runtime);

    assertThat(json)
        .contains("\"labelKey\":\"platform.nav.references\"")
        .contains("\"labelKey\":\"platform.nav.games\"")
        .contains("\"value\":\"/app/platform/catalog/games\"")
        .contains("\"children\"")
        .doesNotContain("\"labelKey\":\"catalog\"");
  }
}
