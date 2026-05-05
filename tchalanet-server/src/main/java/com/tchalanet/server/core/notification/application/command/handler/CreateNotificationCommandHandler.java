package com.tchalanet.server.core.notification.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.NotificationDeliveryId;
import com.tchalanet.server.common.types.id.NotificationId;
import com.tchalanet.server.core.notification.application.command.model.CreateNotificationCommand;
import com.tchalanet.server.core.notification.application.port.out.NotificationDeliveryWriterPort;
import com.tchalanet.server.core.notification.application.port.out.NotificationWriterPort;
import com.tchalanet.server.core.notification.domain.model.Notification;
import com.tchalanet.server.core.notification.domain.model.NotificationAction;
import com.tchalanet.server.core.notification.domain.model.NotificationChannel;
import com.tchalanet.server.core.notification.domain.model.NotificationDelivery;
import com.tchalanet.server.core.notification.domain.model.NotificationDeliveryStatus;
import com.tchalanet.server.core.notification.domain.model.NotificationStatus;
import java.time.Clock;
import java.util.Set;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class CreateNotificationCommandHandler
    implements VoidCommandHandler<CreateNotificationCommand> {

  private final Clock clock;
  private final IdGenerator idGenerator;
  private final NotificationWriterPort notificationWriter;
  private final NotificationDeliveryWriterPort deliveryWriter;

  @Override
  @TchTx
  public void handle(CreateNotificationCommand command) {
    if (command.dedupeKey() != null && !command.dedupeKey().isBlank()) {
      var existing = notificationWriter.findByDedupeKey(command.dedupeKey());
      if (existing.isPresent()) {
        return;
      }
    }

    var now = clock.instant();
    var notificationId = NotificationId.of(idGenerator.newUuid());
    var notification =
        new Notification(
            notificationId,
            command.tenantId(),
            command.sourceType(),
            command.sourceId(),
            command.dedupeKey(),
            command.audienceType(),
            command.audienceValue(),
            command.severity(),
            command.kind(),
            command.category(),
            command.titleKey(),
            command.messageKey(),
            command.titleText(),
            command.messageText(),
            command.payload(),
            new NotificationAction(command.actionType(), command.actionUrl()),
            NotificationStatus.UNREAD,
            null,
            null,
            command.expiresAt(),
            now,
            now);

    var saved = notificationWriter.save(notification);
    scheduleDeliveries(saved, command.channels(), now);
  }

  private void scheduleDeliveries(
      Notification notification, Set<NotificationChannel> channels, java.time.Instant now) {
    var requested = channels == null || channels.isEmpty() ? Set.of(NotificationChannel.WEB) : channels;
    for (var channel : requested) {
      deliveryWriter.save(
          new NotificationDelivery(
              NotificationDeliveryId.of(idGenerator.newUuid()),
              notification.tenantId(),
              notification.id(),
              channel,
              notification.audienceValue(),
              NotificationDeliveryStatus.PENDING,
              0,
              now,
              null,
              null,
              null,
              null,
              null,
              notification.payload(),
              now,
              now));
    }
  }
}
