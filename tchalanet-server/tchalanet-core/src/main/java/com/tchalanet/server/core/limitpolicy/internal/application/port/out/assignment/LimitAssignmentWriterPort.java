package com.tchalanet.server.core.limitpolicy.internal.application.port.out.assignment;

import com.tchalanet.server.common.types.id.LimitAssignmentId;
import com.tchalanet.server.core.limitpolicy.internal.domain.model.LimitAssignment;

import java.time.Instant;

public interface LimitAssignmentWriterPort {

    LimitAssignment save(LimitAssignment assignment);

    void softDelete(LimitAssignmentId id, Instant deletedAt);
}
