package com.tchalanet.server.core.notification.infra.external;

import com.tchalanet.server.common.notification.model.NotificationTarget;
import com.tchalanet.server.common.notification.model.SendNotificationPayload;
import com.tchalanet.server.common.types.enums.NotificationChannel;
import com.tchalanet.server.common.types.enums.NotificationType;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.notification.infra.config.EdgeNotificationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EdgeNotificationGatewayAdapterTest {

    @Mock
    private EdgeHmacSigner hmacSigner;

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private EdgeNotificationProperties properties;
    private EdgeNotificationGatewayAdapter adapter;

    @BeforeEach
    void setUp() {
        properties = new EdgeNotificationProperties(
            true,
            "http://localhost:3000",
            "/internal/notifications/send",
            "test-secret",
            Duration.ofSeconds(2),
            Duration.ofSeconds(5)
        );
        adapter = new EdgeNotificationGatewayAdapter(properties, hmacSigner, restClient);

        lenient().when(restClient.post()).thenReturn(requestBodyUriSpec);
        lenient().when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.body(anyString())).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        lenient().when(responseSpec.toBodilessEntity()).thenReturn(null);
    }

    @Test
    void shouldSendSlackNotificationWithCorrectRecipient() {
        var signedRequest = new EdgeHmacSigner.SignedRequest(
            "2026-05-04T12:00:00Z",
            "sha256=abc123",
            "{\"eventId\":\"evt_123\"}"
        );
        when(hmacSigner.sign(anyString(), any())).thenReturn(signedRequest);

        var payload = new SendNotificationPayload(
            NotificationType.BATCH_MESSAGE,
            NotificationChannel.SLACK,
            null,
            Locale.ENGLISH,
            Map.of("channelKey", "batch-draws", "message", "Test message")
        );

        adapter.send(payload);

        verify(requestBodySpec).contentType(MediaType.APPLICATION_JSON);
        verify(requestBodySpec).header(eq("X-Tch-Timestamp"), eq("2026-05-04T12:00:00Z"));
        verify(requestBodySpec).header(eq("X-Tch-Signature"), eq("sha256=abc123"));
        verify(requestBodySpec).body("{\"eventId\":\"evt_123\"}");
    }

    @Test
    void shouldSendEmailNotificationWithToField() {
        var signedRequest = new EdgeHmacSigner.SignedRequest(
            "2026-05-04T12:00:00Z",
            "sha256=abc123",
            "{\"eventId\":\"evt_123\"}"
        );
        when(hmacSigner.sign(anyString(), any())).thenReturn(signedRequest);

        var tenantId = new TenantId(UUID.randomUUID());
        var userId = new UserId(UUID.randomUUID());
        var target = new NotificationTarget(tenantId, userId, "user@example.com");

        var payload = new SendNotificationPayload(
            NotificationType.TICKET_RECEIPT,
            NotificationChannel.EMAIL,
            target,
            Locale.FRENCH,
            Map.of("ticketNumber", "TK-123")
        );

        adapter.send(payload);

        verify(hmacSigner).sign(eq("test-secret"), any());
        verify(requestBodySpec).body("{\"eventId\":\"evt_123\"}");
    }

    @Test
    void shouldNotSendWhenDisabled() {
        var disabledProperties = new EdgeNotificationProperties(
            false,
            "http://localhost:3000",
            "/internal/notifications/send",
            "test-secret",
            Duration.ofSeconds(2),
            Duration.ofSeconds(5)
        );
        var disabledAdapter = new EdgeNotificationGatewayAdapter(
            disabledProperties, hmacSigner, restClient
        );

        var payload = new SendNotificationPayload(
            NotificationType.BATCH_MESSAGE,
            NotificationChannel.SLACK,
            null,
            Locale.ENGLISH,
            Map.of("message", "Test")
        );

        disabledAdapter.send(payload);

        // Should not call restClient when disabled
        verify(restClient, org.mockito.Mockito.never()).post();
    }

    @Test
    void shouldIncludeHmacHeaders() {
        var signedRequest = new EdgeHmacSigner.SignedRequest(
            "2026-05-04T12:00:00Z",
            "sha256=abc123def456",
            "{\"eventId\":\"evt_789\"}"
        );
        when(hmacSigner.sign(anyString(), any())).thenReturn(signedRequest);

        var payload = new SendNotificationPayload(
            NotificationType.BATCH_MESSAGE,
            NotificationChannel.SLACK,
            null,
            Locale.ENGLISH,
            Map.of("channelKey", "batch-draws")
        );

        adapter.send(payload);

        verify(requestBodySpec).header(eq("X-Request-Id"), any());
        verify(requestBodySpec).header(eq("Idempotency-Key"), any());
        verify(requestBodySpec).header(eq("X-Tch-Timestamp"), eq("2026-05-04T12:00:00Z"));
        verify(requestBodySpec).header(eq("X-Tch-Signature"), eq("sha256=abc123def456"));
    }
}

