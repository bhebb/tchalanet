package com.tchalanet.server.features.pagemodel.shared.init;

import com.fasterxml.jackson.databind.JsonNode;
import com.tchalanet.server.common.util.JsonUtils;
import com.tchalanet.server.features.pagemodel.shared.PageModelRepository;
import com.tchalanet.server.features.pagemodel.shared.PageModelService;
import com.tchalanet.server.features.pagemodel.shared.PageModelType;
import com.tchalanet.server.features.pagemodel.shared.template.PageModelTemplateEntity;
import com.tchalanet.server.features.pagemodel.shared.template.PageModelTemplateService;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PageModelBootstrapService {

  private final PageModelRepository repository;
  private final PageModelTemplateService templateService;
  private final PageModelService pageModelService;
  private final JsonUtils jsonUtils;

  /** Alias historique : bootstrapForTenant -> seedDefaultsForTenant */
  public void bootstrapForDefaultTenant() {
    seedDefaultsForTenant();
  }

  public void seedDefaultsForDefaultTenant() {
    seedDefaultsForTenant();
  }

  public void seedDefaultsForTenant() {
    for (PageModelType type : PageModelType.values()) {
      try {
        var existing = repository.findAllByLogicalId(type.logicalId());
        if (existing.isEmpty()) {
          ensureTemplateAndCreateInstance(type);
        }
      } catch (DataAccessException dae) {
        // Could be that the table hasn't been created yet (Flyway hasn't run). Skip seeding.
        // Log at debug to keep startup logs clean.
        log.error(
            "PageModelBootstrapService: skipping seed because repository access failed: "
                + dae.getMessage(),
            dae);
        return;
      }
    }
  }

  private void ensureTemplateAndCreateInstance(PageModelType type) {
    // 1) try to find default system template in DB
    var tplOpt = templateService.findDefaultByLogicalId(type.logicalId());
    PageModelTemplateEntity tpl;
    if (tplOpt.isPresent()) {
      tpl = tplOpt.get();
    } else {
      // 2) try to load JSON template from resources and create a system template
      var path = "pagemodel/" + type.logicalId() + ".json";
      var resource = new ClassPathResource(path);
      if (!resource.exists()) {
        // nothing to seed
        return;
      }
      try (InputStream is = resource.getInputStream()) {

        // parse JSON using JsonUtils
        var jsonTree = jsonUtils.readValue(is, JsonNode.class);

        // Try to map the JSON directly to PageModelTemplateEntity when possible.
        // The JSON files may contain only the model/schema but may also contain metadata
        // like code/name/label. We prefer mapping; if mapping fails or yields null for
        // required fields we fill them from PageModelType defaults.
        PageModelTemplateEntity createdTpl = null;
        try {
          createdTpl = jsonUtils.treeToValue(jsonTree, PageModelTemplateEntity.class);
        } catch (Exception ignore) {
          // mapping not possible/practical -> fallback to manual creation
        }

        if (createdTpl == null) createdTpl = new PageModelTemplateEntity();

        // system template
        createdTpl.setTenantId(null);

        // logicalId: prefer JSON value, otherwise use type
        if (createdTpl.getLogicalId() == null || createdTpl.getLogicalId().isBlank()) {
          createdTpl.setLogicalId(type.logicalId());
        }

        // code: prefer JSON, otherwise derive from logicalId
        if (createdTpl.getCode() == null || createdTpl.getCode().isBlank()) {
          createdTpl.setCode(createdTpl.getLogicalId());
        }

        // label / name: try JSON fields, otherwise default to slug/name from type
        if (createdTpl.getName() == null || createdTpl.getName().isBlank()) {
          createdTpl.setName(type.name());
        }
        if (createdTpl.getLabel() == null || createdTpl.getLabel().isBlank()) {
          createdTpl.setLabel(type.slug());
        }

        // schemaVersion default
        if (createdTpl.getSchemaVersion() == 0) createdTpl.setSchemaVersion(1);

        // model: prefer JSON 'model' node or whole file as model
        if (createdTpl.getModel() == null) {
          // if jsonTree has a top-level 'model' node, use it, otherwise use the whole json as model
          if (jsonTree.has("model")) {
            createdTpl.setModel(jsonTree.get("model"));
          } else {
            createdTpl.setModel(jsonTree);
          }
        }

        // schema: ensure non-null (DB column is NOT NULL)
        if (createdTpl.getSchema() == null) {
          if (jsonTree.has("schema")) {
            createdTpl.setSchema(jsonTree.get("schema"));
          } else {
            createdTpl.setSchema(jsonUtils.emptyObjectNode());
          }
        }

        createdTpl.setDescription(
            createdTpl.getDescription()); // keep whatever value present (may be null)
        createdTpl.setDefault(true);
        createdTpl.setSystem(true);
        // Ensure audit timestamps in case auditing is not yet active
        Instant now = Instant.now();
        createdTpl.setCreatedAt(now);
        createdTpl.setUpdatedAt(now);
        tpl = templateService.create(createdTpl);
      } catch (IOException e) {
        throw new IllegalStateException("Unable to read template resource: " + type.logicalId(), e);
      }
    }

    // 3) create a PUBLISHED instance for this tenant based on the template
    pageModelService.createFromTemplate(tpl, true);
  }
}
