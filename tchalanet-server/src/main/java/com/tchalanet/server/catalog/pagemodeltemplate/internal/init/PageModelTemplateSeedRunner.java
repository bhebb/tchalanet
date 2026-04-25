package com.tchalanet.server.catalog.pagemodeltemplate.internal.init;

import com.fasterxml.jackson.databind.JsonNode;
import com.tchalanet.server.catalog.pagemodeltemplate.api.model.PageModelTemplateLevel;
import com.tchalanet.server.catalog.pagemodeltemplate.api.model.PageModelTemplateView;
import com.tchalanet.server.catalog.pagemodeltemplate.internal.write.PageModelTemplateAdminService;
import com.tchalanet.server.common.util.JsonUtils;
import com.tchalanet.server.core.pagemodel.domain.model.PageModelType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
@Slf4j
@Order(10)
public class PageModelTemplateSeedRunner implements ApplicationRunner {

    private final PageModelTemplateAdminService adminService;
    private final JsonUtils jsonUtils;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Seeding page model templates from classpath...");

        for (PageModelType type : PageModelType.values()) {
            try {
                log.info("Seeding logicalId={}", type.logicalId());
                var createdOrUpdated = upsertFromClasspath(type.logicalId());
                if (createdOrUpdated != null) {
                    log.info("Seed OK logicalId={} code={} level={}",
                        createdOrUpdated.logicalId(),
                        createdOrUpdated.code(),
                        createdOrUpdated.level());
                } else {
                    log.debug("No seed file for logicalId={}", type.logicalId());
                }
            } catch (Exception e) {
                log.error("Seed FAILED logicalId={}", type.logicalId(), e);
            }
        }

        log.info("Page model template seeding complete.");
    }

    private PageModelTemplateView upsertFromClasspath(String logicalId) throws Exception {
        var path = "pagemodel/" + logicalId + ".json";
        var resource = new ClassPathResource(path);
        if (!resource.exists()) return null;

        try (InputStream is = resource.getInputStream()) {
            JsonNode jsonTree = jsonUtils.readValue(is, JsonNode.class);

            JsonNode schema = jsonTree.has("schema") ? jsonTree.get("schema") : jsonUtils.emptyObjectNode();
            JsonNode model = jsonTree.has("model") ? jsonTree.get("model") : jsonTree;

            String code = jsonTree.hasNonNull("code") ? jsonTree.get("code").asText() : logicalId;
            String name = jsonTree.hasNonNull("name") ? jsonTree.get("name").asText() : logicalId;
            String label = jsonTree.hasNonNull("label") ? jsonTree.get("label").asText() : logicalId;
            String description = jsonTree.hasNonNull("description") ? jsonTree.get("description").asText() : null;
            int schemaVersion = jsonTree.hasNonNull("schemaVersion") ? jsonTree.get("schemaVersion").asInt() : 1;

            var seed = new PageModelTemplateView(
                null,
                code,
                logicalId,
                name,
                label,
                description,
                schema,
                model,
                schemaVersion,
                true,
                PageModelTemplateLevel.GLOBAL,
                null,   // tenantId
                null,   // createdAt
                null    // updatedAt
            );

            return adminService.upsertGlobalFromSeed(seed);
        }
    }
}
