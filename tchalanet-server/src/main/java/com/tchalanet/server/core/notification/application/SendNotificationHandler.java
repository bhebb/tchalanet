package com.tchalanet.server.core.notification.application;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.enums.NotificationChannel;
import com.tchalanet.server.core.notification.domain.InvalidNotificationException;
import com.tchalanet.server.common.notification.model.NotificationTarget;
import com.tchalanet.server.common.notification.model.SendNotificationPayload;
import com.tchalanet.server.common.notification.NotificationGatewayPort;

import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;

/** Handler d'application pour l'envoi de notifications. */
@UseCase
@RequiredArgsConstructor
public class SendNotificationHandler implements VoidCommandHandler<SendNotificationCommand> {

  private final NotificationGatewayPort notificationGatewayPort;

  @Override
  @TchTx // ou pas, selon si tu veux tracer en DB plus tard
  public void handle(SendNotificationCommand command) {
    validate(command);

    var target = new NotificationTarget(command.tenantId(), command.userId(), command.recipient());

    var payload =
        new SendNotificationPayload(
            command.type(),
            command.channel(),
            target,
            Locale.of(command.locale()),
            Map.copyOf(command.data()));

    notificationGatewayPort.send(payload);
  }

  private void validate(SendNotificationCommand cmd) {
    if (cmd.channel() == NotificationChannel.SMS || cmd.channel() == NotificationChannel.WHATSAPP) {
      if (!isPhoneNumber(cmd.recipient())) {
        throw new InvalidNotificationException("Recipient must be phone number for SMS/WhatsApp");
      }
    }
    // autres règles : locale non vide, tenantId non nul etc.
  }

  private boolean isPhoneNumber(String value) {
    // validation minimale, à raffiner
    return value != null && value.matches("\\+?[0-9]{6,15}");
  }
}
