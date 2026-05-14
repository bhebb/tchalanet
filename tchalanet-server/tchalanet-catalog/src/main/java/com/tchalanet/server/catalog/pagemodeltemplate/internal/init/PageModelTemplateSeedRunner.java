package com.tchalanet.server.catalog.pagemodeltemplate.internal.init;

import tools.jackson.databind.JsonNode;
import com.tchalanet.server.catalog.pagemodeltemplate.api.model.PageModelTemplateLevel;
import com.tchalanet.server.catalog.pagemodeltemplate.api.model.PageModelTemplateView;
import com.tchalanet.server.catalog.pagemodeltemplate.internal.write.PageModelTemplateAdminService;
import com.tchalanet.server.common.json.utils.JsonUtils;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

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

        for (Resource resource : loadTemplateResources()) {
            var logicalId = logicalIdFrom(resource);
            try {
                log.info("Seeding logicalId={}", logicalId);
                var createdOrUpdated = upsertFromClasspath(resource, logicalId);
                log.info("Seed OK logicalId={} code={} level={}",
                    createdOrUpdated.logicalId(),
                    createdOrUpdated.code(),
                    createdOrUpdated.level());
            } catch (Exception e) {
                log.error("Seed FAILED logicalId={}", logicalId, e);
            }
        }

        log.info("Page model template seeding complete.");
    }

    private Resource[] loadTemplateResources() {
        try {
            var resolver = new PathMatchingResourcePatternResolver();
            var resources = resolver.getResources("classpath*:pagemodel/*.json");
            return Arrays.stream(resources)
                .sorted(Comparator.comparing(Resource::getFilename, Comparator.nullsLast(String::compareTo)))
                .toArray(Resource[]::new);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to load page model template resources", e);
        }
    }

    private String logicalIdFrom(Resource resource) {
        var filename = resource.getFilename();
        if (filename == null || !filename.endsWith(".json")) {
            throw new IllegalArgumentException("Invalid page model template resource: " + resource);
        }
        return filename.substring(0, filename.length() - ".json".length());
    }

    private PageModelTemplateView upsertFromClasspath(Resource resource, String logicalId) throws Exception {
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
