package com.tchalanet.server.core.limitpolicy.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.LimitDefinitionId;
import com.tchalanet.server.core.limitpolicy.application.port.out.LimitDefinitionWriterPort;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitDefinition;
import com.tchalanet.server.core.limitpolicy.infra.persistence.mapper.LimitDefinitionMapper;
import com.tchalanet.server.core.limitpolicy.infra.persistence.repository.LimitDefinitionJpaRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LimitDefinitionWriterAdapter implements LimitDefinitionWriterPort {

  private final LimitDefinitionJpaRepository defRepo;
  private final LimitDefinitionMapper mapper;

  @Override
  public LimitDefinition save(LimitDefinition def) {
    var entity = mapper.toEntity(def);
    entity = defRepo.save(entity);
    return mapper.toDomain(entity);
  }

  @Override
  public void softDelete(LimitDefinitionId id) {
    var entity = defRepo.findById(id.value()).orElseThrow();
    entity.setDeletedAt(Instant.now());
    defRepo.save(entity);
  }
}
