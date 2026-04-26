package com.tchalanet.server.core.drawresult.infra.persistence.mapper;

import com.tchalanet.server.common.types.enums.DrawSource;
import com.tchalanet.server.common.types.enums.ResultQuality;
import com.tchalanet.server.core.drawresult.domain.model.DrawResult;
import com.tchalanet.server.core.drawresult.domain.model.DrawResultStatus;
import com.tchalanet.server.core.drawresult.infra.persistence.DrawResultJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DrawResultMapper {

  default DrawResult toDomain(DrawResultJpaEntity e) {
    if (e == null) return null;

    DrawResultStatus status = e.getStatus();
    DrawSource src = e.getSource();
    ResultQuality quality = e.getQuality();

    return new DrawResult(
        e.getOccurredAt(), // [D9] No more Instant.now() fallback here
        status,
        src,
        quality,
        e.getSourceHash(),
        e.getFetchedAt(),
        e.getSourceResult(),
        e.getHaitiResult(),
        e.getRawPayload(),
        e.getOverrideReason());
  }

  default DrawResultJpaEntity toEntity(DrawResult d) {
    if (d == null) return null;
    DrawResultJpaEntity e = new DrawResultJpaEntity();
    e.setOccurredAt(d.occurredAt());
    e.setStatus(d.status());
    e.setSource(d.source());
    e.setQuality(d.quality());
    e.setSourceHash(d.sourceHash());
    e.setFetchedAt(d.fetchedAt());
    e.setSourceResult(d.sourceResult());
    e.setHaitiResult(d.haitiResult());
    e.setRawPayload(d.rawPayload());
    e.setOverrideReason(d.overrideReason());
    return e;
  }
}
