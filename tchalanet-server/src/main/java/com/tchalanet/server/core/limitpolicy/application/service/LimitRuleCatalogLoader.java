package com.tchalanet.server.core.limitpolicy.application.service;

import com.tchalanet.server.core.limitpolicy.application.query.model.rules.LimitRuleCatalog;
import com.tchalanet.server.core.limitpolicy.application.query.model.rules.LimitRuleSpec;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Component
@RequiredArgsConstructor
public class LimitRuleCatalogLoader {

    private final ObjectMapper objectMapper;

    private LimitRuleCatalog catalog;

    @PostConstruct
    void load() {
        try {
            var resource = new ClassPathResource("limitpolicy/rules.v1.json");

            try (var input = resource.getInputStream()) {
                this.catalog = objectMapper.readValue(input, LimitRuleCatalog.class);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load limitpolicy/rules.v1.json", e);
        }
    }

    public List<LimitRuleSpec> listAvailableRules() {
        return catalog.rules();
    }
}
