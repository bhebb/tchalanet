package com.tchalanet.server.core.limitpolicy.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.limitpolicy.application.command.model.CreateLimitDefinitionCommand;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitDefinition;
import com.tchalanet.server.core.limitpolicy.infra.persistence.mapper.LimitDefinitionMapper;
import com.tchalanet.server.core.limitpolicy.infra.persistence.repository.LimitDefinitionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@Component
@RequiredArgsConstructor
public class CreateLimitDefinitionCommandHandler implements CommandHandler<CreateLimitDefinitionCommand, LimitDefinition> {

  private final LimitDefinitionJpaRepository repo;
  private final LimitDefinitionMapper mapper;

  @Override
  @Transactional
  public LimitDefinition handle(CreateLimitDefinitionCommand cmd) {
    LimitDefinition def = new LimitDefinition(
        null, // id
        cmd.tenantId(),
        cmd.ruleKey(),
        cmd.enabled(),
        cmd.onBreach(),
        cmd.params(),
        new LimitDefinition.AppliesTo(cmd.betTypes(), cmd.selectionPattern()),
        0L
    );
    var entity = mapper.toEntity(def);
    entity = repo.save(entity);
    return mapper.toDomain(entity);
  }
}
