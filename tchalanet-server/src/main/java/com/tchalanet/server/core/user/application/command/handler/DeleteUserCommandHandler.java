package com.tchalanet.server.core.user.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.user.application.command.model.DeleteUserCommand;
import com.tchalanet.server.core.user.application.port.out.UserWriterPort;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class DeleteUserCommandHandler implements VoidCommandHandler<DeleteUserCommand> {

  private final UserWriterPort userWriterPort;

  @Override
  @Transactional
  public void handle(DeleteUserCommand command) {
    userWriterPort.softDelete(command.userId(), Instant.now());
  }
}
