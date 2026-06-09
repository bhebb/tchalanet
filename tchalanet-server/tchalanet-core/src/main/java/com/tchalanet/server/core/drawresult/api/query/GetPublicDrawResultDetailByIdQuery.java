package com.tchalanet.server.core.drawresult.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.core.drawresult.api.query.view.PublicDrawResultDetailView;

/**
 * Query publique — retourne le détail complet d'un tirage par son ID opaque.
 * Utilisée par {@code GET /public/draw-results/{drawResultId}}.
 */
public record GetPublicDrawResultDetailByIdQuery(DrawResultId id)
    implements Query<PublicDrawResultDetailView> {}

