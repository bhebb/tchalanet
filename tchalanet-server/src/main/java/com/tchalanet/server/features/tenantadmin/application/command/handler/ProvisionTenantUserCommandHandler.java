package com.tchalanet.server.features.tenantadmin.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.features.tenantadmin.application.command.model.ProvisionTenantUserCommand;
import com.tchalanet.server.features.tenantadmin.application.command.model.ProvisionTenantUserResult;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ProvisionTenantUserCommandHandler implements CommandHandler<ProvisionTenantUserCommand, ProvisionTenantUserResult> {

  // inject CommandBus/QueryBus if orchestration across domains is required

  @Override
  public ProvisionTenantUserResult handle(ProvisionTenantUserCommand command) {
    // Orchestration placeholder: call core.user.CreateUserCommand, then core.tenantuser upsert
    // Implementation left to the feature wiring
    return new ProvisionTenantUserResult(null, false);
  }
}
