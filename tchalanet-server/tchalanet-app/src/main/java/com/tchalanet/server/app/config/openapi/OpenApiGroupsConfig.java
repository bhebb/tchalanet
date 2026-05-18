package com.tchalanet.server.app.config.openapi;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiGroupsConfig {

    @Bean
    public GroupedOpenApi publicApi() {
        return group("public", "/public/**");
    }

    @Bean
    public GroupedOpenApi platformApi() {
        return group("platform", "/platform/**");
    }

    @Bean
    public GroupedOpenApi adminApi() {
        return group("admin", "/admin/**");
    }

    @Bean
    public GroupedOpenApi tenantApi() {
        return group("tenant", "/tenant/**");
    }

    @Bean
    public OpenApiCustomizer sortTagsAlphabetically() {
        return openApi -> {
            if (openApi.getTags() != null) {
                openApi.setTags(openApi.getTags().stream().sorted(java.util.Comparator.comparing(io.swagger.v3.oas.models.tags.Tag::getName)).toList());
            }
        };
    }

    private GroupedOpenApi group(String name, String pattern) {
        return GroupedOpenApi.builder().group(name).pathsToMatch(pattern).build();
    }
}
