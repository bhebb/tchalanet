package com.tchalanet.server.core.limitpolicy.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.limitpolicy.application.command.model.UpdateLimitDefinitionCommand;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitDefinition;
import com.tchalanet.server.core.limitpolicy.infra.persistence.mapper.LimitDefinitionMapper;
import com.tchalanet.server.core.limitpolicy.infra.persistence.repository.LimitDefinitionJpaRepository;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@Component
@RequiredArgsConstructor
public class UpdateLimitDefinitionCommandHandler
    implements CommandHandler<UpdateLimitDefinitionCommand, LimitDefinition> {

  private final LimitDefinitionJpaRepository repo;
  private final LimitDefinitionMapper mapper;

  @Override
  @Transactional
  public LimitDefinition handle(UpdateLimitDefinitionCommand cmd) {
    var entity = repo.findById(cmd.definitionId()).orElseThrow();
    entity.setEnabled(cmd.enabled());
    entity.setOnBreach(cmd.onBreach());
    entity.setParams(cmd.params());
    entity.setAppliesTo(
        Map.of("bet_types", cmd.betTypes(), "selection_pattern", cmd.selectionPattern()));
    entity = repo.save(entity);
    return mapper.toDomain(entity);
  }
}
