package com.tchalanet.server.core.pagemodel.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.PageModelId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.pagemodel.internal.infra.web.dto.PageModelAdminDetailDto;
import jakarta.validation.constraints.NotNull;
import java.util.Optional;

/**
 * Commande de duplication d'un PageModel.
 * Le clone est toujours créé en DRAFT dans le même tenant que la source.
 * Retourne le PageModelAdminDetailDto du clone créé.
 *
 * @param sourceId     ID du PageModel source (obligatoire)
 * @param actorId      utilisateur qui déclenche la duplication
 * @param newLogicalId logicalId de la copie — si absent, suffixe "-copy" appliqué
 * @param newSlug      slug de la copie — si absent, suffixe "-copy" appliqué (ou null si source null)
 */
public record DuplicatePageModelCommand(
    @NotNull PageModelId sourceId,
    UserId actorId,
    Optional<String> newLogicalId,
    Optional<String> newSlug
) implements Command<PageModelAdminDetailDto> {}

