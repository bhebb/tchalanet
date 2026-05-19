package com.tchalanet.server.core.offlinesync.internal.application.port.out;

import com.tchalanet.server.common.types.id.OfflineCodeId;
import com.tchalanet.server.core.offlinesync.internal.domain.model.code.OfflineCode;

import java.util.Optional;

public interface OfflineCodeWriterPort {

    OfflineCode save(OfflineCode code);

    /**
     * Pessimistic lock + load: used by the sync handler to atomically transition
     * AVAILABLE → RESERVED for a given code without race against a concurrent batch.
     */
    Optional<OfflineCode> lockForReservation(OfflineCodeId id);
}
