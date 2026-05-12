package com.tchalanet.server.core.pagemodel.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.NotificationId;
import com.tchalanet.server.common.types.id.UserId;
import jakarta.validation.constraints.NotBlank;
import java.util.Optional;

public record IgnoreTemplateUpdateCommand(
    @NotBlank String logicalId, Optional<NotificationId> notificationId, UserId actorId)
    implements Command<Boolean> {}
