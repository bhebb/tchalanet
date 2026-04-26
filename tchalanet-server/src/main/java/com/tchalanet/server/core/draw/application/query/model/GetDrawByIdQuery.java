package com.tchalanet.server.core.draw.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.core.draw.domain.model.DrawSummary;

/**
 * Query permettant de récupérer un {@link DrawSummary} par son identifiant.
 * Lance une exception {@code 404} si le draw n'existe pas.
 */
public record GetDrawByIdQuery(DrawId id) implements Query<DrawSummary> {}

