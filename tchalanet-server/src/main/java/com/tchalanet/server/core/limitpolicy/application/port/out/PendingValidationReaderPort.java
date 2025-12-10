package com.tchalanet.server.core.limitpolicy.application.port.out;

import com.tchalanet.server.core.limitpolicy.domain.model.PendingValidation;
import java.util.List;
import java.util.UUID;

public interface PendingValidationReaderPort {
    List<PendingValidation> findPendingForTenant(UUID tenantId);
}

