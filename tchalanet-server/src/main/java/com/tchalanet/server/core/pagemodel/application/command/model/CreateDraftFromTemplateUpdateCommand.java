package com.tchalanet.server.core.pagemodel.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.UserId;
import jakarta.validation.constraints.NotBlank;

public record CreateDraftFromTemplateUpdateCommand(@NotBlank String logicalId, UserId actorId)
    implements Command<Boolean> {}
