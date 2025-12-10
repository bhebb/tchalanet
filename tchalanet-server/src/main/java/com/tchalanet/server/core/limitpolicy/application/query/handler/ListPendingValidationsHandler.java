package com.tchalanet.server.core.limitpolicy.application.query.handler;

import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.limitpolicy.application.port.out.PendingValidationReaderPort;
import com.tchalanet.server.core.limitpolicy.application.query.model.PendingValidationDto;
import com.tchalanet.server.core.limitpolicy.domain.model.PendingValidation;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@UseCase
public class ListPendingValidationsHandler {

    private final PendingValidationReaderPort reader;

    @Transactional(readOnly = true)
    public List<PendingValidationDto> handle(UUID tenantId) {
        List<PendingValidation> pendings = reader.findPendingForTenant(tenantId);
        return pendings.stream().map(p -> new PendingValidationDto(
            p.id(),
            p.type(),
            p.target(),
            p.requestedAmount(),
            p.requestedBy(),
            p.requestedAt()
        )).collect(Collectors.toList());
    }
}

