package com.tchalanet.server.core.limitpolicy.infra.persistence;

import com.tchalanet.server.core.limitpolicy.application.port.out.PendingValidationReaderPort;
import com.tchalanet.server.core.limitpolicy.domain.model.PendingValidation;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class PendingValidationReaderAdapter implements PendingValidationReaderPort {

    @Override
    public List<PendingValidation> findPendingForTenant(UUID tenantId) {
        // Minimal implementation for now: return empty list (no pending validations).
        return List.of();
    }
}

