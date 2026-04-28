package com.tchalanet.server.core.session.application.port.out;

import com.tchalanet.server.core.session.domain.model.SalesSessionTotals;

/** Port for writing POS session totals. */
public interface SalesSessionTotalsWriterPort {

  SalesSessionTotals upsert(SalesSessionTotals totals);
}
