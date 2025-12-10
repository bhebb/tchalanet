package com.tchalanet.server.core.tenant.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.tenant.application.command.model.DeactivateTenantCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@UseCase
@RequiredArgsConstructor
@Component
public class DeactivateTenantCommandHandler implements VoidCommandHandler<DeactivateTenantCommand> {

  @Override
  public void handle(DeactivateTenantCommand command) {
    // TODO: implement deactivation
    throw new UnsupportedOperationException("DeactivateTenantCommandHandler not implemented yet");
  }
}

