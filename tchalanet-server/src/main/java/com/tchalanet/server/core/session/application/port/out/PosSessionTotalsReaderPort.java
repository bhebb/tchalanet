package com.tchalanet.server.core.session.application.port.out;

import com.tchalanet.server.core.session.domain.model.PosSessionTotals;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for reading session totals.
 */
public interface PosSessionTotalsReaderPort {

  Optional<PosSessionTotals> findBySessionId(UUID sessionId);
}
