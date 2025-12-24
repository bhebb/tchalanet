package com.tchalanet.server.core.limitpolicy.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.limitpolicy.application.command.model.DeleteLimitDefinitionCommand;
import com.tchalanet.server.core.limitpolicy.infra.persistence.repository.LimitDefinitionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@Component
@RequiredArgsConstructor
public class DeleteLimitDefinitionCommandHandler implements CommandHandler<DeleteLimitDefinitionCommand, Void> {

  private final LimitDefinitionJpaRepository repo;

  @Override
  @Transactional
  public Void handle(DeleteLimitDefinitionCommand cmd) {
    var entity = repo.findById(cmd.definitionId()).orElseThrow();
    entity.setDeletedAt(java.time.Instant.now());
    repo.save(entity);
    return null;
  }
}
