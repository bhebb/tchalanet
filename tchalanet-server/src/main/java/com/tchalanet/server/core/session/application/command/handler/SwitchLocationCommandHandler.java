package com.tchalanet.server.core.session.application.command.handler;

import com.tchalanet.server.common.app.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.session.application.command.model.SwitchLocationCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@UseCase
@RequiredArgsConstructor
@Component
public class SwitchLocationCommandHandler implements VoidCommandHandler<SwitchLocationCommand> {

  @Override
  public void handle(SwitchLocationCommand command) {
    // TODO: implement location switch with permission checks and audit
    throw new UnsupportedOperationException("SwitchLocationCommandHandler not implemented yet");
  }
}

