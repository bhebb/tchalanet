package com.tchalanet.server.core.outlet.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.outlet.application.command.model.UpdateOutletConfigCommand;
import com.tchalanet.server.core.outlet.application.port.out.OutletRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@UseCase
@RequiredArgsConstructor
@Component
public class UpdateOutletConfigHandler implements VoidCommandHandler<UpdateOutletConfigCommand> {

  private final OutletRepositoryPort repo;

  @Override
  public void handle(UpdateOutletConfigCommand command) {
    // TODO: validate and persist outlet config
    throw new UnsupportedOperationException("UpdateOutletConfigHandler not implemented yet");
  }
}

