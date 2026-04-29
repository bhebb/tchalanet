package com.tchalanet.server.core.notification.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.notification.application.command.model.ArchiveNotificationCommand;
import com.tchalanet.server.core.notification.application.port.out.NotificationWriterPort;
import java.time.Clock;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ArchiveNotificationHandler implements VoidCommandHandler<ArchiveNotificationCommand> {

  private final Clock clock;
  private final NotificationWriterPort writer;

  @Override
  @TchTx
  public void handle(ArchiveNotificationCommand command) {
    writer.archive(command.notificationId(), clock.instant());
  }
}
