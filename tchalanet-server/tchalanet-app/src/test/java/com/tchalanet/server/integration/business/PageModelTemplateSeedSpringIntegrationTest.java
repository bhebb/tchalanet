package com.tchalanet.server.integration.business;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.json.utils.JsonUtils;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import tools.jackson.databind.JsonNode;

class PageModelTemplateSeedSpringIntegrationTest extends BusinessRuntimeIntegrationTestBase {

  private static final String TEMPLATE_PATTERN = "classpath*:/pagemodel/templates/*.template.json";

  @Autowired private JsonUtils jsonUtils;

  @Test
  void seedsGlobalTemplatesWithJsonMatchingStaticTemplateFiles() throws Exception {
    var expectedByLogicalId = loadExpectedTemplates();

    var actualLogicalIds =
        jdbc.queryForList(
            """
                select logical_id
                from page_model_template
                where level = 'GLOBAL' and deleted_at is null
                order by logical_id
                """,
            String.class);

    assertThat(actualLogicalIds)
        .containsExactlyElementsOf(expectedByLogicalId.keySet().stream().sorted().toList());

    for (var expected : expectedByLogicalId.values()) {
      var rows =
          jdbc.query(
              """
                  select
                    code,
                    logical_id,
                    scope,
                    slug,
                    name,
                    label,
                    description,
                    schema::text,
                    model::text,
                    schema_version,
                    is_default,
                    level,
                    tenant_id
                  from page_model_template
                  where logical_id = ? and level = 'GLOBAL' and deleted_at is null
                  """,
              (rs, rowNum) ->
                  new DbTemplate(
                      rs.getString("code"),
                      rs.getString("logical_id"),
                      rs.getString("scope"),
                      rs.getString("slug"),
                      rs.getString("name"),
                      rs.getString("label"),
                      rs.getString("description"),
                      jsonUtils.parse(rs.getString("schema")),
                      jsonUtils.parse(rs.getString("model")),
                      rs.getInt("schema_version"),
                      rs.getBoolean("is_default"),
                      rs.getString("level"),
                      rs.getObject("tenant_id")),
              expected.logicalId());

      assertThat(rows).as("template row for %s", expected.logicalId()).hasSize(1);
      var actual = rows.get(0);

      assertThat(actual)
          .usingRecursiveComparison()
          .ignoringFields("schema", "model")
          .isEqualTo(expected.toDbMetadata());
      assertThat(actual.schema())
          .as("%s schema", expected.logicalId())
          .isEqualTo(expected.schema());
      assertThat(actual.model()).as("%s model", expected.logicalId()).isEqualTo(expected.model());
    }
  }

  private Map<String, ExpectedTemplate> loadExpectedTemplates() throws Exception {
    var resolver = new PathMatchingResourcePatternResolver();
    var resources = resolver.getResources(TEMPLATE_PATTERN);

    assertThat(resources).as("static PageModel template files").isNotEmpty();

    var templates = new LinkedHashMap<String, ExpectedTemplate>();
    for (Resource resource : resources) {
      var expected = readExpectedTemplate(resource);
      assertThat(templates)
          .as("duplicate static template logicalId=%s", expected.logicalId())
          .doesNotContainKey(expected.logicalId());
      templates.put(expected.logicalId(), expected);
    }
    return templates;
  }

  private ExpectedTemplate readExpectedTemplate(Resource resource) throws Exception {
    try (InputStream in = resource.getInputStream()) {
      var node = jsonUtils.parse(in);
      return new ExpectedTemplate(
          requiredText(node, "code", resource),
          requiredText(node, "logicalId", resource),
          requiredText(node, "scope", resource),
          requiredText(node, "slug", resource),
          requiredText(node, "name", resource),
          requiredText(node, "label", resource),
          textOrNull(node, "description"),
          objectOrEmpty(node.get("schema")),
          requiredNode(node, "model", resource),
          intOrDefault(node.get("schemaVersion"), 1),
          boolOrDefault(node),
          textOrDefault(node.get("level"), "GLOBAL"),
          node.get("tenantId"));
    }
  }

  private String requiredText(JsonNode node, String field, Resource resource) {
    String value = textOrNull(node, field);
    assertThat(value).as("required field %s in %s", field, resource.getDescription()).isNotBlank();
    return value.trim();
  }

  private String textOrNull(JsonNode node, String field) {
    JsonNode value = node.get(field);
    return value == null || value.isNull() ? null : value.asString();
  }

  private String textOrDefault(JsonNode node, String fallback) {
    return node == null || node.isNull() || node.asString().isBlank() ? fallback : node.asString();
  }

  private JsonNode requiredNode(JsonNode node, String field, Resource resource) {
    JsonNode value = node.get(field);
    assertThat(value).as("required field %s in %s", field, resource.getDescription()).isNotNull();
    assertThat(value.isNull())
        .as("required field %s in %s", field, resource.getDescription())
        .isFalse();
    return value;
  }

  private JsonNode objectOrEmpty(JsonNode node) {
    return node == null || node.isNull() ? jsonUtils.parse("{}") : node;
  }

  private int intOrDefault(JsonNode node, int fallback) {
    return node == null || node.isNull() ? fallback : node.asInt();
  }

  private boolean boolOrDefault(JsonNode node) {
    JsonNode value = node.get("isDefault");
    if (value == null || value.isNull()) {
      value = node.get("default");
    }
    return value == null || value.isNull() || value.asBoolean();
  }

  private record ExpectedTemplate(
      String code,
      String logicalId,
      String scope,
      String slug,
      String name,
      String label,
      String description,
      JsonNode schema,
      JsonNode model,
      int schemaVersion,
      boolean isDefault,
      String level,
      JsonNode tenantId) {
    DbTemplate toDbMetadata() {
      var expectedTenantId = tenantId == null || tenantId.isNull() ? null : tenantId.asString();
      return new DbTemplate(
          code,
          logicalId,
          scope,
          slug,
          name,
          label,
          description,
          null,
          null,
          schemaVersion,
          isDefault,
          level,
          expectedTenantId);
    }
  }

  private record DbTemplate(
      String code,
      String logicalId,
      String scope,
      String slug,
      String name,
      String label,
      String description,
      JsonNode schema,
      JsonNode model,
      int schemaVersion,
      boolean isDefault,
      String level,
      Object tenantId) {}
}
