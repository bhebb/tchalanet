package com.tchalanet.server.core.tenant.application.command.handler;

import com.tchalanet.server.common.app.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.tenant.application.command.model.ActivateTenantCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@UseCase
@RequiredArgsConstructor
@Component
public class ActivateTenantCommandHandler implements VoidCommandHandler<ActivateTenantCommand> {

  @Override
  public void handle(ActivateTenantCommand command) {
    // TODO: implement activation
    throw new UnsupportedOperationException("ActivateTenantCommandHandler not implemented yet");
  }
}

