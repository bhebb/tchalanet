package com.tchalanet.server.core.pagemodel.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.PageModelId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.pagemodel.infra.web.dto.PageModelAdminDetailDto;
import jakarta.validation.constraints.NotNull;

/**
 * Commande de réinitialisation d'un PageModel vers les valeurs du template lié.
 * Retourne le PageModelAdminDetailDto de l'instance réinitialisée.
 */
public record ResetPageModelCommand(
    @NotNull PageModelId id,
    UserId actorId
) implements Command<PageModelAdminDetailDto> {}

