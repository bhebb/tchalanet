package com.tchalanet.server.core.pagemodel.internal.application.command.handler;


import com.tchalanet.server.catalog.pagemodeltemplate.api.PageModelTemplateCatalog;
import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.PageModelId;
import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.core.pagemodel.api.command.UpsertPageModelCommand;
import com.tchalanet.server.core.pagemodel.internal.application.port.out.PageModelReadPort;
import com.tchalanet.server.core.pagemodel.internal.application.port.out.PageModelWritePort;
import com.tchalanet.server.core.pagemodel.internal.application.service.JsonSchemaValidatorUtil;
import com.tchalanet.server.core.pagemodel.internal.domain.exception.PageModelSchemaViolationException;
import com.tchalanet.server.core.pagemodel.internal.domain.model.PageModelInstance;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;

import java.time.Clock;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class UpsertPageModelCommandHandler
    implements CommandHandler<UpsertPageModelCommand, PageModelInstance> {

    private final Clock clock;
    private final PageModelReadPort readPort;
    private final PageModelWritePort writePort;
    private final PageModelTemplateCatalog templateCatalog;
    private final JsonUtils jsonUtils;
    private final IdGenerator idGenerator;
    private final JsonSchemaValidatorUtil jsonSchemaValidatorUtil;

    @Override
    @TchTx
    public PageModelInstance handle(UpsertPageModelCommand cmd) {
        var now = clock.instant();

        validateAgainstTemplate(cmd.logicalId(), cmd.modelJson());

        PageModelInstance inst;
        JsonNode node = cmd.modelJson();

        if (node != null && node.isTextual()) {
            node = jsonUtils.parse(node.asText());
        }

        if (cmd.id().isEmpty()) {
            inst = PageModelInstance.createDraft(
                PageModelId.of(idGenerator.newUuid()),
                cmd.tenantId(),
                cmd.logicalId(),
                cmd.scope(),
                cmd.slug(),
                cmd.schemaVersion() == null ? 1 : cmd.schemaVersion(),
                node,
                null,
                now,
                cmd.actorId()
            );
        } else {
            PageModelId pid = cmd.id().get();

            inst = readPort.findById(pid).orElseThrow();

            inst.applyUpsert(
                cmd.scope(),
                cmd.slug(),
                cmd.schemaVersion() == null ? inst.schemaVersion() : cmd.schemaVersion(),
                node,
                null,
                now,
                cmd.actorId()
            );
        }

        if (cmd.publish()) {
            inst.markPublished(now, cmd.actorId());
        }

        return writePort.save(inst);
    }

    private void validateAgainstTemplate(String logicalId, JsonNode modelNode) {
        if (logicalId == null || modelNode == null) {
            return;
        }

        templateCatalog.findByLogicalId(logicalId).ifPresent(template -> {
            JsonNode schemaNode = template.schema();

            var errors = jsonSchemaValidatorUtil.validate(schemaNode, modelNode);

            if (!errors.isEmpty()) {
                var violations = errors.stream()
                    .map(e -> new PageModelSchemaViolationException.Violation(
                        e.getInstanceLocation().toString(),
                        e.getMessage()
                    ))
                    .toList();

                throw new PageModelSchemaViolationException(logicalId, violations);
            }
        });
    }
}
