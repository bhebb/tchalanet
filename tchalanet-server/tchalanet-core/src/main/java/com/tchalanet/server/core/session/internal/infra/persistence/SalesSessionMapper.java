package com.tchalanet.server.core.session.internal.infra.persistence;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.session.internal.domain.model.SalesSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Mapper between SalesSession domain model and JPA entity. */
@Component
@RequiredArgsConstructor
public class SalesSessionMapper {

    public SalesSessionJpaEntity toEntity(SalesSession session) {
        var entity = new SalesSessionJpaEntity();
        applyToEntity(session, entity);
        return entity;
    }

    public void applyToEntity(SalesSession session, SalesSessionJpaEntity entity) {
        entity.setId(session.id().value());
        entity.setTenantId(session.tenantId().value());
        entity.setOutletId(session.outletId().value());
        entity.setTerminalId(session.terminalId() == null ? null : session.terminalId().value());

        entity.setOpenedBy(session.openedBy().value());
        entity.setOpenedAt(session.openedAt());
        entity.setBusinessDate(session.businessDate());

        entity.setStatus(session.status());

        entity.setClosedBy(session.closedBy() == null ? null : session.closedBy().value());
        entity.setClosedAt(session.closedAt());
        entity.setCloseReason(session.closeReason());

        entity.setOpeningFloatCents(session.openingFloatCents());
        entity.setExpectedClosingAmountCents(session.expectedClosingAmountCents());
        entity.setDeclaredClosingAmountCents(session.declaredClosingAmountCents());
        entity.setVarianceCents(session.varianceCents());

        entity.setFinalizedAt(session.finalizedAt());
        entity.setFinalizedBy(session.finalizedBy() == null ? null : session.finalizedBy().value());
        entity.setFinalizeReason(session.finalizeReason());
    }

    public SalesSession toDomain(SalesSessionJpaEntity entity) {
        return SalesSession.load(
            SalesSessionId.of(entity.getId()),
            TenantId.of(entity.getTenantId()),
            OutletId.of(entity.getOutletId()),
            TerminalId.nullableOf(entity.getTerminalId()),
            UserId.of(entity.getOpenedBy()),
            entity.getOpenedAt(),
            entity.getBusinessDate(),
            entity.getStatus(),
            UserId.nullableOf(entity.getClosedBy()),
            entity.getClosedAt(),
            entity.getCloseReason(),
            entity.getOpeningFloatCents(),
            entity.getExpectedClosingAmountCents(),
            entity.getDeclaredClosingAmountCents(),
            entity.getVarianceCents(),
            entity.getFinalizedAt(),
            UserId.nullableOf(entity.getFinalizedBy()),
            entity.getFinalizeReason());
    }
}
