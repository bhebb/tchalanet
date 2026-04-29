package com.tchalanet.server.core.notification.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.notification.application.command.model.ExpireNotificationsCommand;
import com.tchalanet.server.core.notification.application.port.out.NotificationWriterPort;
import java.time.Clock;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ExpireNotificationsHandler
    implements CommandHandler<ExpireNotificationsCommand, Integer> {

  private final Clock clock;
  private final NotificationWriterPort writer;

  @Override
  @TchTx
  public Integer handle(ExpireNotificationsCommand command) {
    var now = command.now() == null ? clock.instant() : command.now();
    return writer.expire(now);
  }
}
