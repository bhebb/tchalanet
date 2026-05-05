package com.tchalanet.server.core.drawresult.infra.persistence.mapper;

import com.tchalanet.server.common.types.enums.DrawSource;
import com.tchalanet.server.common.types.enums.ResultQuality;
import com.tchalanet.server.core.drawresult.domain.model.DrawResult;
import com.tchalanet.server.core.drawresult.domain.model.DrawResultStatus;
import com.tchalanet.server.core.drawresult.infra.persistence.DrawResultJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DrawResultMapper {

  default DrawResult toDomain(DrawResultJpaEntity entity) {
    if (entity == null) return null;

    DrawResultStatus status = entity.getStatus();
    DrawSource src = entity.getSource();
    ResultQuality quality = entity.getQuality();

    return new DrawResult(
        entity.getOccurredAt(), // [D9] No more Instant.now() fallback here
        status,
        src,
        quality,
        entity.getSourceHash(),
        entity.getFetchedAt(),
        entity.getSourceResult(),
        entity.getHaitiResult(),
        entity.getRawPayload(),
        entity.getOverrideReason());
  }

  default DrawResultJpaEntity toEntity(DrawResult drawResult) {
    if (drawResult == null) return null;
    DrawResultJpaEntity entity = new DrawResultJpaEntity();
    entity.setOccurredAt(drawResult.occurredAt());
    entity.setStatus(drawResult.status());
    entity.setSource(drawResult.source());
    entity.setQuality(drawResult.quality());
    entity.setSourceHash(drawResult.sourceHash());
    entity.setFetchedAt(drawResult.fetchedAt());
    entity.setSourceResult(drawResult.sourceResult());
    entity.setHaitiResult(drawResult.haitiResult());
    entity.setRawPayload(drawResult.rawPayload());
    entity.setOverrideReason(drawResult.overrideReason());
    return entity;
  }
}
