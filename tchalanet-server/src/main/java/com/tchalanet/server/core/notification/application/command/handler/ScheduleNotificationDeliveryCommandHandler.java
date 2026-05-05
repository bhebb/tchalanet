package com.tchalanet.server.core.notification.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.NotificationDeliveryId;
import com.tchalanet.server.core.notification.application.command.model.ScheduleNotificationDeliveryCommand;
import com.tchalanet.server.core.notification.application.port.out.NotificationDeliveryWriterPort;
import com.tchalanet.server.core.notification.domain.model.NotificationDelivery;
import com.tchalanet.server.core.notification.domain.model.NotificationDeliveryStatus;
import java.time.Clock;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ScheduleNotificationDeliveryCommandHandler
    implements CommandHandler<ScheduleNotificationDeliveryCommand, NotificationDeliveryId> {

  private final Clock clock;
  private final IdGenerator idGenerator;
  private final NotificationDeliveryWriterPort writer;

  @Override
  @TchTx
  public NotificationDeliveryId handle(ScheduleNotificationDeliveryCommand command) {
    var now = clock.instant();
    var id = NotificationDeliveryId.of(idGenerator.newUuid());
    var saved =
        writer.save(
            new NotificationDelivery(
                id,
                null,
                command.notificationId(),
                command.channel(),
                command.recipient(),
                NotificationDeliveryStatus.PENDING,
                0,
                command.nextAttemptAt() == null ? now : command.nextAttemptAt(),
                null,
                null,
                null,
                null,
                null,
                command.payload(),
                now,
                now));
    return saved.id();
  }
}
