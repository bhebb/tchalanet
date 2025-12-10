package com.tchalanet.server.core.draw.infra.persistence.mapper;

import com.tchalanet.server.core.draw.domain.model.Draw;
import com.tchalanet.server.core.draw.domain.model.DrawChannelId;
import com.tchalanet.server.core.draw.domain.model.DrawResult;
import com.tchalanet.server.core.draw.domain.model.DrawSource;
import com.tchalanet.server.core.draw.domain.model.DrawStatus;
import com.tchalanet.server.core.draw.infra.persistence.DrawJpaEntity;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DrawMapper {

  default Draw toDomain(DrawJpaEntity entity) {
    if (entity == null) return null;
    var scheduled = entity.getScheduledAt();
    var scheduledZdt =
        scheduled == null ? null : ZonedDateTime.ofInstant(scheduled, ZoneId.of("UTC"));
    var cutoffZdt =
        scheduledZdt == null
            ? null
            : scheduledZdt.minusSeconds(
                entity.getCutoffSec() == null ? 120 : entity.getCutoffSec());

    var status =
        entity.getStatus() == null ? DrawStatus.PLANNED : DrawStatus.valueOf(entity.getStatus());

    // result mapping omitted for first pass
    DrawResult result = null;

    return new Draw(
        entity.getId(),
        entity.getTenantId(),
        new DrawChannelId(entity.getDrawChannelId()),
        scheduledZdt,
        cutoffZdt,
        status,
        DrawSource.valueOf(entity.getDrawSource()),
        result);
  }

  default DrawJpaEntity toEntity(Draw domain) {
    DrawJpaEntity entity = new DrawJpaEntity();
    if (domain == null) return entity;
    entity.setId(domain.id());
    entity.setTenantId(domain.tenantId());
    entity.setDrawChannelId(domain.channelId() == null ? null : domain.channelId().value());
    // only set fields that exist on domain.Draw
    if (domain.scheduledAt() != null)
      entity.setScheduledAt(java.time.Instant.from(domain.scheduledAt()));
    if (domain.cutoffAt() != null && domain.scheduledAt() != null) {
      long cutoffSec =
          java.time.Duration.between(domain.cutoffAt(), domain.scheduledAt()).getSeconds();
      entity.setCutoffSec((int) cutoffSec);
    }
    if (domain.status() != null) entity.setStatus(domain.status().name());
    // drawSource/resultPayload/systemGenerated/locked are set elsewhere when available
    return entity;
  }
}
