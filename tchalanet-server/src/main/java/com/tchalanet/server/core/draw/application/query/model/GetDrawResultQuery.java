// ...existing code...
package com.tchalanet.server.core.draw.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.ResultSlotId;

import java.time.Instant;
import java.util.Optional;

/** Query to lookup a DrawResultId by resultSlotId and occurredAt. */
public record GetDrawResultQuery(ResultSlotId resultSlotId, Instant occurredAt)
    implements Query<Optional<com.tchalanet.server.common.types.id.DrawResultId>> {}
