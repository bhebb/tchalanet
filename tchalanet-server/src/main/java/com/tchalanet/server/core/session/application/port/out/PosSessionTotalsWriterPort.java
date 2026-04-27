package com.tchalanet.server.core.session.application.port.out;

import com.tchalanet.server.core.session.domain.model.PosSessionTotals;

/** Port for writing POS session totals. */
public interface PosSessionTotalsWriterPort {

  PosSessionTotals upsert(PosSessionTotals totals);
}
