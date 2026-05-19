package com.tchalanet.server.core.offlinesync.internal.application.port.out;

import com.tchalanet.server.common.types.id.OfflineGrantId;
import com.tchalanet.server.core.offlinesync.internal.domain.model.grant.OfflineGrant;

import java.util.Optional;

public interface OfflineGrantWriterPort {

    /** Persist a new grant or apply a state transition. */
    OfflineGrant save(OfflineGrant grant);

    /**
     * Pessimistic-write reload of a grant within the current transaction. Used by the sync
     * handler to serialise quota increments against concurrent batches.
     */
    Optional<OfflineGrant> lockForUpdate(OfflineGrantId id);
}
