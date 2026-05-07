package com.tchalanet.server.core.pagemodel.application.command.handler;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.notification.application.command.model.ArchiveNotificationCommand;
import com.tchalanet.server.core.pagemodel.application.command.model.IgnoreTemplateUpdateCommand;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class IgnoreTemplateUpdateCommandHandler
    implements CommandHandler<IgnoreTemplateUpdateCommand, Boolean> {

  private final CommandBus commandBus;

  @Override
  @TchTx
  public Boolean handle(IgnoreTemplateUpdateCommand command) {
    command
        .notificationId()
        .ifPresent(id -> commandBus.execute(new ArchiveNotificationCommand(id, command.actorId())));
    return true;
  }
}
