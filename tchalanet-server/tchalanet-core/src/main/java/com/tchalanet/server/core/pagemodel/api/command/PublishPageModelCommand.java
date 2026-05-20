package com.tchalanet.server.core.pagemodel.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.PageModelId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;

// [Phase 2A-1] ajout de tenantId + actorId pour supprimer TchContext.get() des handlers (analysis §BLOQUANT)
public record PublishPageModelCommand(
    PageModelId id,
    TenantId tenantId,
    UserId actorId
) implements Command<Void> {}
