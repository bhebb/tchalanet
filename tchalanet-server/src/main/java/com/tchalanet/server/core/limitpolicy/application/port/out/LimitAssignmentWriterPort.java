package com.tchalanet.server.core.limitpolicy.application.port.out;
import com.tchalanet.server.common.types.id.TenantId;

import com.tchalanet.server.core.limitpolicy.domain.model.LimitAssignment;

import java.util.UUID;

public interface LimitAssignmentWriterPort {

    LimitAssignment save(LimitAssignment assignment);

    void softDelete(TenantId tenantId, UUID assignmentId);
}
