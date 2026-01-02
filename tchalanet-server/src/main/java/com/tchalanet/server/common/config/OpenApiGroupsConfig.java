package com.tchalanet.server.common.config;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiGroupsConfig {

  @Bean
  public GroupedOpenApi publicApi() {
    return group("public", "/public/**", null);
  }

  @Bean
  public GroupedOpenApi platformApi() {
    return group("platform", "/platform/**", null);
  }

  @Bean
  public GroupedOpenApi adminApi() {
    return group("admin", "/admin/**", null);
  }

  @Bean
  public GroupedOpenApi tenantApi() {
    return group("tenant", "/tenant/**", null);
  }

  @Bean
  public GroupedOpenApi sdrApi() {
    return group("sdr", "/_sdr/**", normalizeSdrTags());
  }

  @Bean
  public OpenApiCustomizer sortTagsAlphabetically() {
    return openApi -> {
      if (openApi.getTags() != null) {
        openApi.setTags(
            openApi.getTags().stream()
                .sorted(java.util.Comparator.comparing(io.swagger.v3.oas.models.tags.Tag::getName))
                .toList());
      }
    };
  }

  private GroupedOpenApi group(String name, String pattern, OpenApiCustomizer customizer) {
    var b = GroupedOpenApi.builder().group(name).pathsToMatch(pattern);
    if (customizer != null) b.addOpenApiCustomizer(customizer);
    return b.build();
  }

  // Retag SDR operations into "SDR • <Resource>"
  private OpenApiCustomizer normalizeSdrTags() {
    return openApi -> {
      if (openApi == null || openApi.getPaths() == null) return;

      openApi
          .getPaths()
          .forEach(
              (path, item) -> {
                if (item == null || path == null) return;
                if (!path.startsWith("/_sdr/")) return;

                String resource = extractResource(path);
                if (resource == null) return;

                String tag = "SDR • " + toTitle(resource);

                for (var op : item.readOperations()) {
                  if (op == null) continue;
                  op.setTags(java.util.List.of(tag));
                }
              });
    };
  }

  private String extractResource(String path) {
    var parts = path.split("/");
    return parts.length >= 3 ? parts[2] : null;
  }

  private String toTitle(String slug) {
    var chunks = slug.split("-");
    var sb = new StringBuilder();
    for (var c : chunks) {
      if (c.isBlank()) continue;
      sb.append(Character.toUpperCase(c.charAt(0))).append(c.substring(1)).append(' ');
    }
    return sb.toString().trim();
  }
}
