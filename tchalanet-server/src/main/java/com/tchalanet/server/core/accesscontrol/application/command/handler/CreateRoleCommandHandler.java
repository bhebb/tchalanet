package com.tchalanet.server.core.accesscontrol.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.accesscontrol.application.command.model.CreateRoleCommand;
import com.tchalanet.server.core.accesscontrol.infra.persistence.AppRoleEntity;
import com.tchalanet.server.core.accesscontrol.infra.persistence.AppRoleJpaRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class CreateRoleCommandHandler implements CommandHandler<CreateRoleCommand, UUID> {

  private final AppRoleJpaRepository appRoleRepository;

  @Override
  public UUID handle(CreateRoleCommand command) {
    AppRoleEntity entity = new AppRoleEntity();
    entity.setCode(command.code());
    entity.setName(command.name());
    entity.setDescription(command.description());
    entity.setTenantId(command.tenantId().value());
    // parentRoleId may be null when not provided by the client
    entity.setParentRoleId(command.parentRoleId() == null ? null : command.id().value());
    AppRoleEntity saved = appRoleRepository.save(entity);
    return saved.getId();
  }
}
