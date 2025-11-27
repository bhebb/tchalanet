package com.tchalanet.server.draw.infra.persistence.mapper;

import com.tchalanet.server.draw.domain.model.Draw;
import com.tchalanet.server.draw.infra.persistence.entity.DrawEntity;
import org.springframework.stereotype.Component;

@Component
public class DrawMapper {

  public DrawEntity toEntity(Draw domain) {
    DrawEntity entity = new DrawEntity();
    entity.setId(domain.id());
    entity.setTenantId(domain.tenantId());
    entity.setDrawChannelId(domain.drawChannelId());
    entity.setGameCode(domain.gameCode());
    entity.setDrawSource(domain.drawSource().name()); // Convert enum to String
    entity.setScheduledAt(domain.scheduledAt());
    entity.setCutoffSec(domain.cutoffSec());
    entity.setStatus(domain.status()); // Use enum directly
    // todo result
    // entity.setResultPayload(domain.resultPayload());
    entity.setSystemGenerated(domain.systemGenerated());
    entity.setLocked(domain.locked());
    entity.setCreatedBy(domain.createdBy());
    entity.setUpdatedBy(domain.updatedBy());
    // createdAt, updatedAt, deletedAt are handled by BaseTenantEntity or @PrePersist
    return entity;
  }

  public Draw toDomain(DrawEntity entity) {
    // todo defdf
    return null;
  }
}
