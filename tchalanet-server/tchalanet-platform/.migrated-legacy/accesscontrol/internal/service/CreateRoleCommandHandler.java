package com.tchalanet.server.platform.accesscontrol.internal.service;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.AppRoleJpaEntity;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.AppRoleJpaRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class CreateRoleRequestHandler implements CommandHandler<CreateRoleRequest, UUID> {

  private final AppRoleJpaRepository appRoleRepository;

  @Override
  public UUID handle(CreateRoleRequest command) {
    AppRoleJpaEntity entity = new AppRoleJpaEntity();
    entity.setCode(command.code());
    entity.setName(command.name());
    entity.setDescription(command.description());
    entity.setTenantId(command.tenantId().value());
    // parentRoleId may be null when not provided by the client
    entity.setParentRoleId(command.parentRoleId() == null ? null : command.parentRoleId().value());
    AppRoleJpaEntity saved = appRoleRepository.save(entity);
    return saved.getId();
  }
}

