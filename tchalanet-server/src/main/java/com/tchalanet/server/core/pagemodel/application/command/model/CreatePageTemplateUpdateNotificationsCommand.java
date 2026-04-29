package com.tchalanet.server.core.pagemodel.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.PageModelTemplateId;
import com.tchalanet.server.common.types.id.UserId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.JsonNode;

public record CreatePageTemplateUpdateNotificationsCommand(
    @NotNull PageModelTemplateId templateId,
    @NotBlank String logicalId,
    @NotNull JsonNode newModel,
    int newSchemaVersion,
    UserId actorId)
    implements Command<Integer> {}
