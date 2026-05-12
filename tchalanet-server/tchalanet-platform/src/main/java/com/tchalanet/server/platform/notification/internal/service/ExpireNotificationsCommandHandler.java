package com.tchalanet.server.platform.notification.internal.service;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.platform.notification.api.model.ExpireNotificationsCommand;
import com.tchalanet.server.platform.notification.internal.service.NotificationWriterPort;
import java.time.Clock;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ExpireNotificationsCommandHandler
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
