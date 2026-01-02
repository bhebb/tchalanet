package com.tchalanet.server.core.draw.infra.persistence.mapper;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.domain.model.DrawResult;
import com.tchalanet.server.core.draw.domain.model.DrawSource;
import com.tchalanet.server.core.draw.infra.persistence.DrawResultJpaEntity;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DrawResultMapper {

  default DrawResult toDomain(DrawResultJpaEntity e) {
    if (e == null) return null;
    List<String> numbersMain =
        e.getNumbersMain() == null ? Collections.emptyList() : List.copyOf(e.getNumbersMain());
    List<String> numbersExtra =
        e.getNumbersExtra() == null ? Collections.emptyList() : List.copyOf(e.getNumbersExtra());

    return new DrawResult(
        DrawSource.valueOf(e.getSource()),
        numbersMain,
        numbersExtra,
        e.getOccurredAt() == null ? Instant.now() : e.getOccurredAt(),
        e.getRawPayload() == null ? null : e.getRawPayload().toString(),
        e.getOverriddenAt() != null,
        e.getOverrideReason());
  }

  default DrawResultJpaEntity toEntity(TenantId tenantId, DrawResult d) {
    if (d == null) return null;
    DrawResultJpaEntity e = new DrawResultJpaEntity();
    e.setTenantId(tenantId.uuid());
    e.setSource(d.source().name());
    e.setStatus(d.overridden() ? "OVERRIDDEN" : "VALID");
    e.setNumbersMain(d.numbersMain() == null ? null : List.copyOf(d.numbersMain()));
    e.setNumbersExtra(d.numbersExtra() == null ? null : List.copyOf(d.numbersExtra()));
    // raw payload in entity is a json map; store the raw string under key 'raw' for compatibility
    e.setRawPayload(Map.of("raw", d.rawPayload()));
    // occurredAt should be persisted if provided
    if (d.occurredAt() != null) {
      e.setOccurredAt(d.occurredAt());
    }
    if (d.overridden()) {
      e.setOverriddenAt(Instant.now());
      e.setOverrideReason(d.overrideReason());
    }
    return e;
  }
}
