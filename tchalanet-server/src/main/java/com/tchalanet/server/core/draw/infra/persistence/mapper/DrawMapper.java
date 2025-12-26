package com.tchalanet.server.core.draw.infra.persistence.mapper;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.core.draw.domain.model.Draw;
import com.tchalanet.server.core.draw.domain.model.DrawResult;
import com.tchalanet.server.core.draw.domain.model.DrawSource;
import com.tchalanet.server.core.draw.domain.model.DrawStatus;
import com.tchalanet.server.core.draw.infra.persistence.DrawJpaEntity;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import com.tchalanet.server.core.draw.application.port.out.DrawResultReaderPort;

/**
 * Mapper between persistence entities and domain Draw objects.
 * Uses the dedicated DrawChannelMapper and DrawResultMapper to map nested objects.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = {DrawChannelMapper.class, DrawResultMapper.class})
public abstract class DrawMapper {

    @Autowired
    protected DrawChannelMapper drawChannelMapper;

    @Autowired
    protected DrawResultReaderPort drawResultReaderPort;

    public Draw toDomain(DrawJpaEntity entity) {
        if (entity == null) return null;

        var scheduled = entity.getScheduledAt();
        var scheduledZdt = scheduled == null ? null : ZonedDateTime.ofInstant(scheduled, ZoneId.of("UTC"));
        var cutoffZdt =
            scheduledZdt == null
                ? null
                : scheduledZdt.minusSeconds(entity.getCutoffSec() == null ? 120 : entity.getCutoffSec());

        var status = entity.getStatus() == null ? DrawStatus.SCHEDULED : entity.getStatus();

        // Map drawChannel using dedicated mapper (it returns domain DrawChannel)
        var channel = drawChannelMapper.toDomain(entity.getDrawChannel());

        var source = entity.getDrawSource() == null ? DrawSource.SYSTEM : DrawSource.valueOf(entity.getDrawSource());

        // Try to load result via the reader port (explicitly call repository/port to avoid missing join)
        var result = getDrawResult(entity);

        return new Draw(
            new DrawId(entity.getId()),
            TenantId.of(entity.getTenantId()),
            channel,
            scheduledZdt,
            cutoffZdt,
            status,
            source,
            result);
    }

    private @Nullable DrawResult getDrawResult(DrawJpaEntity entity) {
        DrawResult result = null;
        if (drawResultReaderPort != null) {
            var maybe = drawResultReaderPort.findByDrawId(TenantId.of(entity.getTenantId()), new DrawId(entity.getId()));
            if (maybe != null && maybe.isPresent()) {
                result = maybe.get();
            }
        }
        return result;
    }

    public DrawJpaEntity toEntity(Draw domain) {
        var entity = new DrawJpaEntity();
        if (domain == null) return entity;

        entity.setId(domain.id().value());
        entity.setTenantId(domain.tenantId().uuid());

        // map drawChannel domain -> entity using DrawChannelMapper
        entity.setDrawChannel(drawChannelMapper.toEntity(domain.drawChannel()));

        // only set fields that exist on domain.Draw
        entity.setScheduledAt(java.time.Instant.from(domain.scheduledAt()));

        if (domain.cutoffAt() != null && domain.scheduledAt() != null) {
            long cutoffSec = java.time.Duration.between(domain.cutoffAt(), domain.scheduledAt()).getSeconds();
            entity.setCutoffSec((int) cutoffSec);
        }
        entity.setStatus(domain.status());

        // drawSource/resultPayload/systemGenerated/locked are set elsewhere when available
        return entity;
    }
}
