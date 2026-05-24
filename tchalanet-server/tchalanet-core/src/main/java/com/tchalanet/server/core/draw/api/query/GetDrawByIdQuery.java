package com.tchalanet.server.core.draw.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.DrawId;

import java.util.Objects;

/**
 * Query permettant de récupérer un {@link DrawSummary} par son identifiant.
 * Lance une exception {@code 404} si le draw n'existe pas.
 */
public record GetDrawByIdQuery(DrawId id) implements Query<DrawSummary> {
    public GetDrawByIdQuery {
        Objects.requireNonNull(id, "id is required");
    }
}

