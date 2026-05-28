package com.tchalanet.server.catalog.pagemodeltemplate.internal.init;

import com.tchalanet.server.catalog.pagemodeltemplate.api.model.PageModelTemplateLevel;
import com.tchalanet.server.catalog.pagemodeltemplate.api.model.PageModelTemplateView;
import com.tchalanet.server.common.json.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Loads static template wrappers from classpath:/pagemodel/templates/*.template.json.
 * <p>
 * These JSON files are catalog template definitions:
 * metadata + schema + model.
 * <p>
 * Belongs to catalog.pagemodeltemplate because it seeds catalog templates,
 * not tenant PageModel instances.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StaticPageModelTemplateLoader {

    private static final String TEMPLATE_PATTERN = "classpath*:/pagemodel/templates/*.template.json";

    private final JsonUtils jsonUtils;

    public List<PageModelTemplateView> loadTemplates() {
        try {
            var resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(TEMPLATE_PATTERN);

            var out = new ArrayList<PageModelTemplateView>();

            for (Resource resource : resources) {
                out.add(readTemplate(resource));
            }

            out.sort(Comparator.comparing(PageModelTemplateView::logicalId));
            return List.copyOf(out);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to load PageModelTemplate static resources", e);
        }
    }

    private PageModelTemplateView readTemplate(Resource resource) {
        try (InputStream in = resource.getInputStream()) {
            String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            JsonNode node = jsonUtils.parse(json);

            return PageModelTemplateView.initFromFile(
                requiredText(node, "code", resource),
                requiredText(node, "logical_id", resource),
                requiredText(node, "scope", resource),
                requiredText(node, "slug", resource),
                requiredText(node, "name", resource),
                requiredText(node, "label", resource),
                textOrNull(node, "description"),
                objectOrEmpty(node),
                requiredNode(node, resource),
                intOrDefault(node),
                boolOrDefault(node),
                levelOrDefault(node)
            );
        } catch (Exception e) {
            throw new IllegalStateException("Invalid PageModelTemplate resource: " + resource, e);
        }
    }

    private JsonNode requiredNode(JsonNode node, Resource resource) {
        JsonNode value = node.get("model");
        if (value == null || value.isNull()) {
            throw new IllegalStateException("Missing field model in " + resource);
        }
        return value;
    }

    private String requiredText(JsonNode node, String field, Resource resource) {
        String value = textOrNull(node, field);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing field `" + field + "` in " + resource);
        }
        return value.trim();
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asString();
    }

    private int intOrDefault(JsonNode node) {
        JsonNode value = node.get("schema_version");
        return value == null || value.isNull() ? 1 : value.asInt();
    }

    private boolean boolOrDefault(JsonNode node) {
        var value = node.get("is_default");
        return value == null || value.isNull() || value.asBoolean();
    }

    private JsonNode objectOrEmpty(JsonNode node) {
        JsonNode value = node.get("schema");
        return value == null || value.isNull() ? jsonUtils.parse("{}") : value;
    }

    private PageModelTemplateLevel levelOrDefault(JsonNode node) {
        JsonNode value = node.get("level");
        if (value == null || value.isNull() || value.asString().isBlank()) {
            return PageModelTemplateLevel.GLOBAL;
        }
        return PageModelTemplateLevel.valueOf(value.asString().trim().toUpperCase());
    }
}
