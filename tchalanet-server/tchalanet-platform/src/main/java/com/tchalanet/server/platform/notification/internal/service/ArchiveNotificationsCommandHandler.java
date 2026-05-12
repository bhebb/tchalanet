package com.tchalanet.server.platform.notification.internal.service;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.platform.notification.api.model.ArchiveNotificationsCommand;
import com.tchalanet.server.platform.notification.internal.service.NotificationWriterPort;
import java.time.Clock;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ArchiveNotificationsCommandHandler implements VoidCommandHandler<ArchiveNotificationsCommand> {

  private final Clock clock;
  private final NotificationWriterPort writer;

  @Override
  @TchTx
  public void handle(ArchiveNotificationsCommand command) {
    writer.archive(command.notificationIds(), clock.instant());
  }
}
