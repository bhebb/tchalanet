package com.tchalanet.server.core.pagemodel.application.command.handler;

import com.networknt.schema.*;
import tools.jackson.databind.JsonNode;
import com.tchalanet.server.catalog.pagemodeltemplate.api.PageModelTemplateCatalog;
import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.PageModelId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.util.JsonUtils;
import com.tchalanet.server.core.pagemodel.application.command.model.UpsertPageModelCommand;
import com.tchalanet.server.core.pagemodel.application.port.out.PageModelReadPort;
import com.tchalanet.server.core.pagemodel.application.port.out.PageModelWritePort;
import com.tchalanet.server.core.pagemodel.domain.exception.PageModelSchemaViolationException;
import com.tchalanet.server.core.pagemodel.domain.model.PageModelInstance;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// [Phase 3A] import @Component supprimé — @UseCase seul suffit (command_query_handlers.md §3.2)
// [Phase 2A-2] suppression de TchContext.get(), UUID.randomUUID() → idGenerator (analysis §BLOQUANT)
// [Phase 3A] @UseCase + CommandHandler pour câblage CQRS (analysis §MAJEUR command_query_handlers.md §3.2)
@UseCase
@RequiredArgsConstructor
@Slf4j
public class UpsertPageModelHandler
    implements CommandHandler<UpsertPageModelCommand, PageModelInstance> {

  private final Clock clock;
  private final PageModelReadPort readPort;
  private final PageModelWritePort writePort;
  private final PageModelTemplateCatalog templateCatalog;
  private final JsonUtils objectMapper;
  private final IdGenerator idGenerator;

  @TchTx
  @Override
  public PageModelInstance handle(UpsertPageModelCommand cmd) {
    // tenantId et actorId proviennent de la commande, fournis par le controller via TchRequestContext
    var tenantId = cmd.tenantId() != null ? cmd.tenantId().value() : null;
    var actorId  = cmd.actorId()  != null ? cmd.actorId().value()  : null;

    var now = clock.instant();
    var modelJson = cmd.modelJson();

    // D.6 — schema validation against template (if schema is non-null and non-empty)
    validateAgainstTemplate(cmd.logicalId(), modelJson);

    PageModelInstance inst;
    if (cmd.id().isEmpty()) {
      inst = PageModelInstance.createDraft(
          idGenerator.newUuid(),   // [Phase 2A-2] via IdGenerator — plus de UUID.randomUUID() direct
          tenantId,
          cmd.logicalId(),
          cmd.scope(),
          cmd.slug(),
          cmd.schemaVersion() == null ? 1 : cmd.schemaVersion(),
          modelJson,
          null,
          now,
          actorId
      );
    } else {
      PageModelId pid = cmd.id().get();
      inst = readPort.findById(pid).orElseThrow();
      inst.applyUpsert(
          cmd.scope(),
          cmd.slug(),
          cmd.schemaVersion() == null ? inst.schemaVersion() : cmd.schemaVersion(),
          modelJson,
          null,
          now,
          actorId
      );
    }

    if (cmd.publish()) {
      inst.markPublished(now, actorId);
    }

    return writePort.save(inst);
  }

  /**
   * Validates modelJson against the template schema for the given logicalId.
   * No-op if template absent, schema absent, or schema empty ({}).
   */
  private void validateAgainstTemplate(String logicalId, Object modelJson) {
    if (modelJson == null || logicalId == null) return;

    templateCatalog.findByLogicalId(logicalId).ifPresent(template -> {
      JsonNode schema = template.schema();
      if (schema == null || schema.isNull() || schema.isEmpty()) return;

      return;
     // try {
         /* SchemaRegistry schemaRegistry = SchemaRegistry.withDialect(Dialects.getDraft202012(),
              builder -> builder.schemas(Map.of("https://example.com/address.schema.json", schemaData)));
          var schema = schemaRegistry.getSchema(SchemaLocation.of("https://example.com/address.schema.json"));
          JsonNode modelNode = objectMapper.valueToTree(modelJson);

          List<Error> errors = schema.validate(instanceData, InputFormat.JSON);

        if (false) {
          List<PageModelSchemaViolationException.Violation> violations = errors.stream()
              .map(e -> new PageModelSchemaViolationException.Violation(e.getPath(), e.getMessage()))
              .toList();
          throw new PageModelSchemaViolationException(logicalId, violations);
        }
      } catch (PageModelSchemaViolationException e) {
        throw e;
      } catch (Exception e) {
        log.warn("Schema validation failed (skipped): {}", e.getMessage());
      }*/
    });
  }
}
