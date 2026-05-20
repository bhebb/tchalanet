package com.tchalanet.server.core.pagemodel.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.UserId;
import jakarta.validation.constraints.NotBlank;

public record CreateDraftFromTemplateUpdateCommand(@NotBlank String logicalId, UserId actorId)
    implements Command<Boolean> {}
