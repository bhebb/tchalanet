package com.tchalanet.server.core.accesscontrol.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.accesscontrol.application.command.model.UpdateRoleCommand;
import com.tchalanet.server.core.accesscontrol.infra.persistence.AppRoleEntity;
import com.tchalanet.server.core.accesscontrol.infra.persistence.AppRoleJpaRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class UpdateRoleCommandHandler implements CommandHandler<UpdateRoleCommand, UUID> {

  private final AppRoleJpaRepository appRoleRepository;

  @Override
  public UUID handle(UpdateRoleCommand command) {
    AppRoleEntity entity =
        appRoleRepository
            .findById(command.id().uuid())
            .orElseThrow(() -> new IllegalArgumentException("Role not found: " + command.id()));
    entity.setCode(command.code());
    entity.setName(command.name());
    entity.setDescription(command.description());
    entity.setTenantId(command.tenantId().uuid());
    // parentRoleId may be null when not provided by the client
    entity.setParentRoleId(command.parentRoleId() == null ? null : command.parentRoleId().uuid());
    AppRoleEntity saved = appRoleRepository.save(entity);
    return saved.getId();
  }
}
