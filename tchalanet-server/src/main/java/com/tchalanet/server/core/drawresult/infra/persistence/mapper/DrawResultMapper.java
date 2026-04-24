package com.tchalanet.server.core.drawresult.infra.persistence.mapper;

import com.tchalanet.server.common.types.enums.ResultQuality;
import com.tchalanet.server.core.drawresult.domain.model.DrawResult;
import com.tchalanet.server.core.drawresult.domain.model.DrawResultStatus;
import com.tchalanet.server.core.drawresult.domain.model.DrawSource;
import com.tchalanet.server.core.drawresult.infra.persistence.DrawResultJpaEntity;
import java.time.Instant;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DrawResultMapper {

  default DrawResult toDomain(DrawResultJpaEntity e) {
    if (e == null) return null;

    // status is already an enum on the JPA entity
    DrawResultStatus status = e.getStatus();
    DrawSource src = e.getSource();

    // quality is already an enum on the JPA entity
    ResultQuality quality = e.getQuality();

    return new DrawResult(
        /* provider */ null,
        /* providerSlot */ null,
        e.getOccurredAt() == null ? Instant.now() : e.getOccurredAt(),
        status,
        src,
        quality,
        e.getSourceHash(),
        e.getFetchedAt() == null ? Instant.now() : e.getFetchedAt(),
        e.getSourceResult(),
        e.getHaitiResult(),
        e.getRawPayload(),
        e.getOverrideReason());
  }

  default DrawResultJpaEntity toEntity(DrawResult d) {
    if (d == null) return null;
    DrawResultJpaEntity e = new DrawResultJpaEntity();
    e.setOccurredAt(d.occurredAt());
    // JPA entity expects enums for status and quality
    e.setStatus(d.status());
    e.setSource(d.source() == null ? null : d.source());
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
