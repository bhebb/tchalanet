package com.tchalanet.server.core.session.infra.persistence.mapper;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.util.JsonbUtils;
import com.tchalanet.server.core.session.domain.model.PosSession;
import com.tchalanet.server.core.session.domain.model.PosSessionStatus;
import com.tchalanet.server.core.session.infra.persistence.entity.PosSessionJpaEntity;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Mapper between PosSession domain model and JPA entity. */
@Component
@RequiredArgsConstructor
public class PosSessionMapper {

  private final JsonbUtils jsonbUtils;

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
        entity.getOpeningFloat() != null
            ? entity.getOpeningFloat().multiply(BigDecimal.valueOf(100)).longValue()
            : BigDecimal.ZERO.longValue(),
        entity.getClosingAmount() != null
            ? entity.getClosingAmount().multiply(BigDecimal.valueOf(100)).longValue()
            : BigDecimal.ZERO.longValue(),
        BigDecimal.ZERO, // totalStake not mapped
        0L, // totalTickets not mapped
        BigDecimal.ZERO, // totalPayout not mapped
        entity.getMeta() == null ? null : jsonbUtils.toJsonOrNull(entity.getMeta()),
        entity.getVersion());
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
    entity.setOpeningFloat(
        domain.openingFloatCents() != null
            ? BigDecimal.valueOf(domain.openingFloatCents())
                .divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP)
            : BigDecimal.ZERO);
    entity.setClosingAmount(
        domain.closingAmountCents() != null
            ? BigDecimal.valueOf(domain.closingAmountCents())
                .divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP)
            : BigDecimal.ZERO);
    // map meta
    entity.setMeta(
        domain.meta() == null ? jsonbUtils.readTree("{}") : jsonbUtils.readTree(domain.meta()));
    entity.setStatus(PosSessionStatus.OPENED);
    return entity;
  }
}
