package com.tchalanet.server.core.session.application.port.out;

import com.tchalanet.server.common.types.id.SessionId;
import com.tchalanet.server.core.session.domain.model.PosSessionTotals;
import java.util.Optional;

/** Port for reading session totals. */
public interface PosSessionTotalsReaderPort {

  Optional<PosSessionTotals> findBySessionId(SessionId sessionId);
}
