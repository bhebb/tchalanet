package com.tchalanet.server.core.offlinesync.internal.infra.persistence.mapper;

import com.tchalanet.server.common.types.id.OfflineCodeBatchId;
import com.tchalanet.server.common.types.id.OfflineCodeId;
import com.tchalanet.server.common.types.id.OfflineGrantId;
import com.tchalanet.server.common.types.id.OfflineSubmissionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.offlinesync.api.model.code.CodeLifecycle;
import com.tchalanet.server.core.offlinesync.api.model.code.OfflineCodeStatus;
import com.tchalanet.server.core.offlinesync.internal.domain.model.code.CodeIdentity;
import com.tchalanet.server.core.offlinesync.internal.domain.model.code.OfflineCode;
import com.tchalanet.server.core.offlinesync.internal.infra.persistence.OfflineCodeJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class OfflineCodeJpaMapper {

    public OfflineCode toDomain(OfflineCodeJpaEntity e) {
        if (e.getExpiresAt() == null)
            throw new IllegalStateException("offline_code.expires_at must not be null for id=" + e.getId());
        return new OfflineCode(
            new CodeIdentity(
                OfflineCodeId.of(e.getId()),
                TenantId.of(e.getTenantId()),
                OfflineCodeBatchId.of(e.getCodeBatchId()),
                e.getGrantId() != null ? OfflineGrantId.of(e.getGrantId()) : null,
                e.getCode()
            ),
            new CodeLifecycle(
                OfflineCodeStatus.valueOf(e.getStatus()),
                e.getReservedAt(),
                e.getConsumedAt(),
                e.getExpiresAt(),
                e.getOfflineSubmissionId() != null ? OfflineSubmissionId.of(e.getOfflineSubmissionId()) : null,
                e.getTicketId() != null ? TicketId.of(e.getTicketId()) : null
            )
        );
    }

    public OfflineCodeJpaEntity toEntity(OfflineCode c, OfflineCodeJpaEntity target) {
        OfflineCodeJpaEntity e = target != null ? target : new OfflineCodeJpaEntity();
        e.setId(c.identity().id().value());
        e.setTenantId(c.identity().tenantId().value());
        e.setCodeBatchId(c.identity().codeBatchId().value());
        e.setGrantId(c.identity().grantId() != null ? c.identity().grantId().value() : null);
        e.setCode(c.identity().code());
        e.setStatus(c.lifecycle().status().name());
        e.setReservedAt(c.lifecycle().reservedAt());
        e.setConsumedAt(c.lifecycle().consumedAt());
        e.setExpiresAt(c.lifecycle().expiresAt());
        e.setOfflineSubmissionId(c.lifecycle().offlineSubmissionId() != null
            ? c.lifecycle().offlineSubmissionId().value() : null);
        e.setTicketId(c.lifecycle().ticketId() != null ? c.lifecycle().ticketId().value() : null);
        return e;
    }
}
