package com.tchalanet.server.core.session.application.port.out;

import com.tchalanet.server.core.session.domain.model.SalesSession;

/** Port for writing POS sessions. */
public interface SalesSessionWriterPort {

  SalesSession save(SalesSession session);
}
