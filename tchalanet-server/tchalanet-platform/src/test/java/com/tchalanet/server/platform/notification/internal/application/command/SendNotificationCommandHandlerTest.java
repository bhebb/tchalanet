package com.tchalanet.server.platform.notification.internal.application.command;

import com.tchalanet.server.common.communication.api.OutboundMessageGateway;
import com.tchalanet.server.common.communication.api.OutboundMessageRequest;
import com.tchalanet.server.common.types.enums.NotificationType;
import com.tchalanet.server.platform.notification.internal.application.command.handler.SendNotificationCommandHandler;
import com.tchalanet.server.platform.notification.api.SendNotificationCommand;
import com.tchalanet.server.platform.notification.internal.application.mapper.OutboundMessageMapper;
import com.tchalanet.server.platform.notification.internal.application.policy.NotificationPolicy;
import com.tchalanet.server.platform.notification.internal.domain.InvalidNotificationException;
import com.tchalanet.server.platform.notification.internal.domain.model.NotificationChannel;
import com.tchalanet.server.platform.notification.internal.domain.model.NotificationRecipient;
import com.tchalanet.server.platform.notification.internal.domain.model.NotificationSeverity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SendNotificationCommandHandlerTest {

    @Mock
    private OutboundMessageGateway outboundMessageGateway;

    @Mock
    private NotificationPolicy notificationPolicy;

    private SendNotificationCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new SendNotificationCommandHandler(
            outboundMessageGateway,
            new OutboundMessageMapper(),
            notificationPolicy
        );
    }

    @Test
    void shouldSendSlackNotificationSuccessfully() {
        var recipient = new NotificationRecipient(
            NotificationChannel.SLACK,
            null,
            "batch-draws",
            null,
            null
        );

        var command = new SendNotificationCommand(
            NotificationType.SYSTEM_MESSAGE,
            NotificationSeverity.INFO,
            List.of(recipient),
            Locale.ENGLISH,
            "Test notification",
            "This is a test",
            Map.of("extra", "data"),
            null,
            "test-reason"
        );

        var result = handler.handle(command);

        assertThat(result.success()).isTrue();
        assertThat(result.message()).contains("successfully");
        assertThat(result.idempotencyKey()).isNotNull();

        verify(notificationPolicy).validateRecipients(List.of(recipient));
        verify(outboundMessageGateway).send(any(OutboundMessageRequest.class));
    }

    @Test
    void shouldIncludeChannelKeyInPayload() {
        var recipient = new NotificationRecipient(
            NotificationChannel.SLACK,
            null,
            "batch-draws",
            null,
            null
        );

        var command = new SendNotificationCommand(
            NotificationType.SYSTEM_MESSAGE,
            NotificationSeverity.INFO,
            List.of(recipient),
            Locale.ENGLISH,
            "Test",
            "Message",
            Map.of(),
            null,
            null
        );

        handler.handle(command);

        var captor = ArgumentCaptor.forClass(OutboundMessageRequest.class);
        verify(outboundMessageGateway).send(captor.capture());

        var payload = captor.getValue();
        assertThat(payload.metadata()).containsEntry("channelKey", "batch-draws");
        assertThat(payload.metadata()).containsEntry("title", "Test");
        assertThat(payload.metadata()).containsEntry("message", "Message");
        assertThat(payload.metadata()).containsEntry("severity", "INFO");
    }

    @Test
    void shouldSendEmailNotificationWithToField() {
        var recipient = new NotificationRecipient(
            NotificationChannel.EMAIL,
            "user@example.com",
            null,
            null,
            null
        );

        var command = new SendNotificationCommand(
            NotificationType.SYSTEM_MESSAGE,
            NotificationSeverity.WARNING,
            List.of(recipient),
            Locale.FRENCH,
            "Warning",
            "Something happened",
            Map.of(),
            null,
            null
        );

        handler.handle(command);

        var captor = ArgumentCaptor.forClass(OutboundMessageRequest.class);
        verify(outboundMessageGateway).send(captor.capture());

        var payload = captor.getValue();
        assertThat(payload.metadata()).containsEntry("to", "user@example.com");
        assertThat(payload.metadata()).doesNotContainKey("channelKey");
        assertThat(payload.locale()).isEqualTo(Locale.FRENCH);
    }

    @Test
    void shouldSendSmsAndWhatsappNotificationsWithToField() {
        var smsRecipient = new NotificationRecipient(
            NotificationChannel.SMS,
            "+15145550100",
            null,
            null,
            null
        );
        var whatsappRecipient = new NotificationRecipient(
            NotificationChannel.WHATSAPP,
            "+15145550101",
            null,
            null,
            null
        );

        var command = new SendNotificationCommand(
            NotificationType.SYSTEM_MESSAGE,
            NotificationSeverity.WARNING,
            List.of(smsRecipient, whatsappRecipient),
            Locale.FRENCH,
            "Warning",
            "Something happened",
            Map.of(),
            null,
            null
        );

        handler.handle(command);

        var captor = ArgumentCaptor.forClass(OutboundMessageRequest.class);
        verify(outboundMessageGateway, times(2)).send(captor.capture());

        assertThat(captor.getAllValues())
            .extracting(request -> request.recipient().to())
            .containsExactly("+15145550100", "+15145550101");
    }

    @Test
    void shouldUseProvidedIdempotencyKey() {
        var recipient = new NotificationRecipient(
            NotificationChannel.SLACK,
            null,
            "test-channel",
            null,
            null
        );

        var customKey = "custom-idempotency-key-123";
        var command = new SendNotificationCommand(
            NotificationType.SYSTEM_MESSAGE,
            NotificationSeverity.INFO,
            List.of(recipient),
            Locale.ENGLISH,
            "Test",
            "Message",
            Map.of(),
            customKey,
            null
        );

        var result = handler.handle(command);

        assertThat(result.idempotencyKey()).isEqualTo(customKey);

        var captor = ArgumentCaptor.forClass(OutboundMessageRequest.class);
        verify(outboundMessageGateway).send(captor.capture());
        assertThat(captor.getValue().metadata()).containsEntry("idempotencyKey", customKey);
    }

    @Test
    void shouldGenerateIdempotencyKeyIfNotProvided() {
        var recipient = new NotificationRecipient(
            NotificationChannel.SLACK,
            null,
            "test-channel",
            null,
            null
        );

        var command = new SendNotificationCommand(
            NotificationType.SYSTEM_MESSAGE,
            NotificationSeverity.INFO,
            List.of(recipient),
            Locale.ENGLISH,
            "Test",
            "Message",
            Map.of(),
            null, // no idempotency key provided
            null
        );

        var result = handler.handle(command);

        assertThat(result.idempotencyKey())
            .isNotNull()
            .startsWith("SYSTEM_MESSAGE_");
    }

    @Test
    void shouldPropagateValidationFailure() {
        var recipient = new NotificationRecipient(
            NotificationChannel.SLACK,
            null,
            null, // invalid - missing channelKey
            null,
            null
        );

        var command = new SendNotificationCommand(
            NotificationType.SYSTEM_MESSAGE,
            NotificationSeverity.INFO,
            List.of(recipient),
            Locale.ENGLISH,
            "Test",
            "Message",
            Map.of(),
            null,
            null
        );

        doThrow(new InvalidNotificationException("SLACK requires channelKey"))
            .when(notificationPolicy).validateRecipients(any());

        assertThatThrownBy(() -> handler.handle(command))
            .isInstanceOf(InvalidNotificationException.class)
            .hasMessageContaining("SLACK requires channelKey");

        verify(outboundMessageGateway, never()).send(any());
    }

    @Test
    void shouldSendToMultipleRecipients() {
        var slackRecipient = new NotificationRecipient(
            NotificationChannel.SLACK,
            null,
            "ops-channel",
            null,
            null
        );

        var emailRecipient = new NotificationRecipient(
            NotificationChannel.EMAIL,
            "admin@example.com",
            null,
            null,
            null
        );

        var command = new SendNotificationCommand(
            NotificationType.SYSTEM_MESSAGE,
            NotificationSeverity.ERROR,
            List.of(slackRecipient, emailRecipient),
            Locale.ENGLISH,
            "Critical Error",
            "System failure detected",
            Map.of(),
            null,
            "critical-alert"
        );

        var result = handler.handle(command);

        assertThat(result.success()).isTrue();
        verify(outboundMessageGateway, times(2)).send(any(OutboundMessageRequest.class));
    }

    @Test
    void shouldIncludeReasonWhenProvided() {
        var recipient = new NotificationRecipient(
            NotificationChannel.SLACK,
            null,
            "test-channel",
            null,
            null
        );

        var command = new SendNotificationCommand(
            NotificationType.SYSTEM_MESSAGE,
            NotificationSeverity.INFO,
            List.of(recipient),
            Locale.ENGLISH,
            "Test",
            "Message",
            Map.of(),
            null,
            "manual-ops-test"
        );

        handler.handle(command);

        var captor = ArgumentCaptor.forClass(OutboundMessageRequest.class);
        verify(outboundMessageGateway).send(captor.capture());
        assertThat(captor.getValue().metadata()).containsEntry("reason", "manual-ops-test");
    }

    @Test
    void shouldIncludeContextFields() {
        var recipient = new NotificationRecipient(
            NotificationChannel.SLACK,
            null,
            "test-channel",
            null,
            null
        );

        Map<String, Object> context = Map.of(
            "jobKey", "draw-lifecycle",
            "status", "FAILED",
            "errorCode", "CONNECTION_TIMEOUT"
        );

        var command = new SendNotificationCommand(
            NotificationType.BATCH_MESSAGE,
            NotificationSeverity.ERROR,
            List.of(recipient),
            Locale.ENGLISH,
            "Batch Failed",
            "Draw lifecycle failed",
            context,
            null,
            null
        );

        handler.handle(command);

        var captor = ArgumentCaptor.forClass(OutboundMessageRequest.class);
        verify(outboundMessageGateway).send(captor.capture());

        var data = captor.getValue().metadata();
        assertThat(data).containsEntry("jobKey", "draw-lifecycle");
        assertThat(data).containsEntry("status", "FAILED");
        assertThat(data).containsEntry("errorCode", "CONNECTION_TIMEOUT");
    }

    @Test
    void shouldNotSendWebRecipientToEdge() {
        var recipient = new NotificationRecipient(
            NotificationChannel.WEB,
            null,
            null,
            null,
            null
        );

        var command = new SendNotificationCommand(
            NotificationType.SYSTEM_MESSAGE,
            NotificationSeverity.INFO,
            List.of(recipient),
            Locale.ENGLISH,
            "In-app",
            "Stored in notification center",
            Map.of(),
            null,
            null
        );

        var result = handler.handle(command);

        assertThat(result.success()).isTrue();
        verify(outboundMessageGateway, never()).send(any());
    }
}
