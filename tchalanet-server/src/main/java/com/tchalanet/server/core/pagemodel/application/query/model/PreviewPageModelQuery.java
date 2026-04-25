package com.tchalanet.server.core.pagemodel.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.PageModelId;
import com.tchalanet.server.core.pagemodel.infra.web.dto.PageModelAdminDetailDto;

/**
 * Query de preview admin : charge un PageModel par son ID et le retourne mappé
 * en PageModelAdminDetailDto pour affichage dans le backoffice.
 * Pas de fallback tenant → default : preview = instance exacte demandée.
 */
public record PreviewPageModelQuery(
    PageModelId id
) implements Query<PageModelAdminDetailDto> {}

