package com.tchalanet.server.core.limitpolicy.application.port.out;

import com.tchalanet.server.core.limitpolicy.domain.model.LimitAssignment;

public interface LimitAssignmentWriterPort {

    LimitAssignment save(LimitAssignment assignment);

    void softDelete(UUID tenantId, UUID assignmentId);
}
