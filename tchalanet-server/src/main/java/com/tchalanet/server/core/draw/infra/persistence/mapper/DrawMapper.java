package com.tchalanet.server.core.draw.infra.persistence.mapper;

import com.tchalanet.server.catalog.drawchannel.internal.mapper.DrawChannelMapper;
import com.tchalanet.server.common.types.enums.DrawSource;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.domain.model.Draw;
import com.tchalanet.server.core.draw.domain.model.DrawStatus;
import com.tchalanet.server.core.draw.infra.persistence.DrawJpaEntity;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    uses = {DrawChannelMapper.class})
public abstract class DrawMapper {

  @Autowired protected DrawChannelMapper drawChannelMapper;

  public Draw toDomain(DrawJpaEntity entity) {
    if (entity == null) return null;

    var channel = drawChannelMapper.toDomain(entity.getDrawChannel());

    ZoneId zone =
        (channel != null && channel.timezone() != null) ? channel.timezone() : ZoneId.of("UTC");

    var scheduledZdt =
        entity.getScheduledAt() == null
            ? null
            : ZonedDateTime.ofInstant(entity.getScheduledAt(), zone);

    var cutoffZdt =
        entity.getCutoffAt() == null ? null : ZonedDateTime.ofInstant(entity.getCutoffAt(), zone);

    var status = entity.getStatus() == null ? DrawStatus.SCHEDULED : entity.getStatus();

    DrawSource source =
        entity.getResultSource() == null ? DrawSource.SYSTEM : entity.getResultSource();

    var resultId =
        entity.getDrawResultId() == null ? null : DrawResultId.of(entity.getDrawResultId());

    return new Draw(
        new DrawId(entity.getId()),
        TenantId.of(entity.getTenantId()),
        channel,
        scheduledZdt,
        cutoffZdt,
        status,
        source,
        resultId);
  }

  public DrawJpaEntity toEntity(Draw domain) {
    var entity = new DrawJpaEntity();
    if (domain == null) return entity;

    entity.setId(domain.id().value());
    entity.setTenantId(domain.tenantId().value());

    var channelEntity = drawChannelMapper.toEntityDefault(domain.drawChannel());
    entity.setDrawChannel(channelEntity);

    entity.setScheduledAt(domain.scheduledAt() == null ? null : domain.scheduledAt().toInstant());
    entity.setCutoffAt(domain.cutoffAt() == null ? null : domain.cutoffAt().toInstant());
    entity.setStatus(domain.status());

    if (domain.drawResultId() != null) {
      entity.setDrawResultId(domain.drawResultId().value());
    }

    return entity;
  }
}
