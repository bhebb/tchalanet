package com.tchalanet.server.platform.notification.internal.service;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.notification.application.command.model.MarkNotificationReadCommand;
import com.tchalanet.server.core.notification.application.port.out.NotificationWriterPort;
import java.time.Clock;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class MarkNotificationReadCommandHandler
    implements VoidCommandHandler<MarkNotificationReadCommand> {

  private final Clock clock;
  private final NotificationWriterPort writer;

  @Override
  @TchTx
  public void handle(MarkNotificationReadCommand command) {
    writer.markRead(command.notificationId(), clock.instant());
  }
}
