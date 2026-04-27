package com.tchalanet.server.core.session.application.port.out;

import com.tchalanet.server.core.session.domain.model.PosSession;

/** Port for writing POS sessions. */
public interface PosSessionWriterPort {

  PosSession save(PosSession session);
}
