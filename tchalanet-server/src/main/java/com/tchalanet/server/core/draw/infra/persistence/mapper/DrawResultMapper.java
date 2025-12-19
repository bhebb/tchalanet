package com.tchalanet.server.core.draw.infra.persistence.mapper;

import com.tchalanet.server.core.draw.domain.model.DrawResult;
import com.tchalanet.server.core.draw.domain.model.DrawSource;
import com.tchalanet.server.core.draw.infra.persistence.DrawResultJpaEntity;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
        e.getCreatedAt() == null ? Instant.now() : e.getCreatedAt(),
        e.getRawPayload() == null ? null : e.getRawPayload().toString(),
        e.getOverriddenAt() != null,
        e.getOverrideReason());
  }

  default DrawResultJpaEntity toEntity(UUID tenantId, DrawResult d) {
    if (d == null) return null;
    DrawResultJpaEntity e = new DrawResultJpaEntity();
    e.setTenantId(tenantId);
    e.setSource(d.source().name());
    e.setStatus(d.overridden() ? "OVERRIDDEN" : "VALID");
    e.setNumbersMain(d.numbersMain() == null ? null : List.copyOf(d.numbersMain()));
    e.setNumbersExtra(d.numbersExtra() == null ? null : List.copyOf(d.numbersExtra()));
    e.setRawPayload(Map.of("raw", d.rawPayload()));
    if (d.overridden()) {
      e.setOverriddenAt(Instant.now());
      e.setOverrideReason(d.overrideReason());
    }
    return e;
  }
}
