package com.tchalanet.server.pos.infra.persistence.mapper;

import com.tchalanet.server.pos.domain.model.PosSession;
import com.tchalanet.server.pos.infra.persistence.entity.PosSessionEntity;
import org.springframework.stereotype.Component;

@Component
public class PosSessionMapper {

  public PosSessionEntity toEntity(PosSession domain) {
    PosSessionEntity entity = new PosSessionEntity();
    entity.setId(domain.getId());
    entity.setTenantId(domain.getTenantId());
    entity.setTerminalId(domain.getTerminalId());
    entity.setUserId(domain.getUserId());
    entity.setStatus(domain.getStatus());
    entity.setOpenedAt(domain.getOpenedAt());
    entity.setClosedAt(domain.getClosedAt());
    entity.setLastActivityAt(domain.getLastActivityAt());
    entity.setOpeningFloat(domain.getOpeningFloat());
    entity.setClosingAmount(domain.getClosingAmount());
    entity.setTotalTicketsAmount(domain.getTotalTicketsAmount());
    entity.setTotalPayoutAmount(domain.getTotalPayoutAmount());
    entity.setGrossMargin(domain.getGrossMargin());
    return entity;
  }

  public PosSession toDomain(PosSessionEntity entity) {
    // Reconstruct the domain object. This assumes PosSession has a constructor
    // or a static factory method suitable for loading from persistence.
    // For simplicity, we'll use a direct reconstruction here, but a dedicated
    // constructor for persistence is often cleaner.
    // Note: The PosSession domain model currently has a private constructor.
    // A dedicated 'load' factory method or a package-private constructor would be needed.
    // For now, this will require a slight adjustment to PosSession's constructor visibility
    // or adding a 'load' factory method.
    // Let's assume a 'load' factory method for now.
    return PosSession.load( // This method needs to be added to PosSession
        entity.getId(),
        entity.getTenantId(),
        entity.getTerminalId(),
        entity.getUserId(),
        entity.getStatus(),
        entity.getOpenedAt(),
        entity.getClosedAt(),
        entity.getLastActivityAt(),
        entity.getOpeningFloat(),
        entity.getClosingAmount(),
        entity.getTotalTicketsAmount(),
        entity.getTotalPayoutAmount(),
        entity.getGrossMargin());
  }
}
