package com.tchalanet.server.core.session.internal.application.port.out;

import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.session.domain.model.SalesSession;

import java.time.Instant;

/**
 * Port for writing POS sessions.
 */
public interface SalesSessionWriterPort {

    SalesSession save(SalesSession session);

    void finalizeSession(SalesSessionId sessionId, Instant finalizedAt, UserId finalizedBy, String reason);

}
