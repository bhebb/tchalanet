package com.tchalanet.server.core.session.infra.persistence.mapper;

import com.tchalanet.server.core.session.domain.model.PosSession;
import com.tchalanet.server.core.session.infra.persistence.entity.PosSessionJpaEntity;
import com.tchalanet.server.common.types.id.SessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Mapper between PosSession domain model and JPA entity.
 */
@Component
public class PosSessionMapper {

    public PosSession toDomain(PosSessionJpaEntity entity) {
        return PosSession.reconstruct(
            SessionId.of(entity.getId()),
            TenantId.of(entity.getTenantId()),
            OutletId.of(entity.getOutletId()),
            TerminalId.of(entity.getTerminalId()),
            UserId.of(entity.getUserId()),
            entity.getStatus(),
            entity.getOpenedAt(),
            entity.getClosedAt(),
            entity.getOpeningFloat() != null ? entity.getOpeningFloat().multiply(BigDecimal.valueOf(100)).longValue() : null,
            entity.getClosingAmount() != null ? entity.getClosingAmount().multiply(BigDecimal.valueOf(100)).longValue() : null,
            BigDecimal.ZERO, // totalStake not mapped
            0L, // totalTickets not mapped
            BigDecimal.ZERO, // totalPayout not mapped
            Map.of(), // meta not mapped
            entity.getVersion()
        );
    }

    public PosSessionJpaEntity toEntity(PosSession domain) {
        var entity = new PosSessionJpaEntity();
        entity.setId(domain.id().uuid());
        entity.setTenantId(domain.tenantId().uuid());
        entity.setOutletId(domain.outletId().uuid());
        entity.setTerminalId(domain.terminalId().uuid());
        entity.setUserId(domain.userId().uuid());
        entity.setStatus(domain.status());
        entity.setOpenedAt(domain.openedAt());
        entity.setClosedAt(domain.closedAt());
        entity.setOpeningFloat(domain.openingFloatCents() != null ? BigDecimal.valueOf(domain.openingFloatCents()).divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP) : null);
        entity.setClosingAmount(domain.closingAmountCents() != null ? BigDecimal.valueOf(domain.closingAmountCents()).divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP) : null);
        return entity;
    }
}
