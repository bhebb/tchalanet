package com.tchalanet.server.core.pos.application.command.handler;

import com.tchalanet.server.common.app.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.pos.application.command.model.SendPosHeartbeatCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@UseCase
@RequiredArgsConstructor
@Component
public class SendPosHeartbeatCommandHandler implements VoidCommandHandler<SendPosHeartbeatCommand> {

  @Override
  public void handle(SendPosHeartbeatCommand command) {
    // TODO: update device lastSeen/status/battery/version
    throw new UnsupportedOperationException("SendPosHeartbeatCommandHandler not implemented yet");
  }
}

