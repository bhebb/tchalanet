package com.tchalanet.server.core.notification.infra.web;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.types.enums.NotificationType;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.core.notification.application.command.model.SendNotificationCommand;
import com.tchalanet.server.core.notification.domain.model.NotificationChannel;
import com.tchalanet.server.core.notification.domain.model.NotificationRecipient;
import com.tchalanet.server.core.notification.domain.model.NotificationSeverity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Contrôleur ops pour tester les notifications.
 * SUPER_ADMIN uniquement.
 */
@RestController
@RequestMapping("/api/v1/ops/notifications")
@Tag(name = "Ops • Notifications")
@RequiredArgsConstructor
@Slf4j
public class OpsNotificationController {

    private final CommandBus commandBus;

    @Operation(
        summary = "Test notification sending (SUPER_ADMIN only)",
        description = "Sends a test notification through edge-service. " +
            "Supports Slack (channelKey), Email (to), SMS (to)."
    )
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping("/test")
    public ResponseEntity<ApiResponse<SendNotificationTestResponse>> testNotification(
        @Valid @RequestBody SendNotificationTestRequest request
    ) {
        log.info("Ops notification test request: channel={}, severity={}",
            request.channel, request.severity);

        try {
            var recipient = new NotificationRecipient(
                request.channel,
                request.to,
                request.channelKey,
                null, // no tenant for ops test
                null  // no user for ops test
            );

            var command = new SendNotificationCommand(
                NotificationType.SYSTEM_MESSAGE, // ops test uses SYSTEM_MESSAGE type
                request.severity,
                List.of(recipient),
                Locale.ENGLISH, // default to English for ops tests
                request.title,
                request.message,
                request.context != null ? request.context : Map.of(),
                null, // let handler generate idempotency key
                "ops-test" // reason
            );

            var result = commandBus.send(command);

            var response = new SendNotificationTestResponse(
                result.success(),
                result.message(),
                result.idempotencyKey(),
                request.channel.name()
            );

            if (result.success()) {
                return ResponseEntity.ok(ApiResponse.success(response));
            } else {
                return ResponseEntity.ok(ApiResponse.warn(response,
                    com.tchalanet.server.common.web.api.ApiNotice.error(
                        "NOTIFICATION_FAILED",
                        result.message()
                    )
                ));
            }

        } catch (Exception e) {
            log.error("Ops notification test failed", e);
            var errorResponse = new SendNotificationTestResponse(
                false,
                "Test failed: " + e.getMessage(),
                null,
                request.channel.name()
            );
            return ResponseEntity.ok(ApiResponse.warn(errorResponse,
                com.tchalanet.server.common.web.api.ApiNotice.error(
                    "TEST_ERROR",
                    e.getMessage()
                )
            ));
        }
    }

    /**
     * Request DTO pour tester l'envoi de notifications.
     */
    public record SendNotificationTestRequest(
        @NotNull NotificationChannel channel,
        @Nullable String to,
        @Nullable String channelKey,
        @NotNull NotificationSeverity severity,
        @NotBlank String title,
        @NotBlank String message,
        @Nullable Map<String, Object> context
    ) {}

    /**
     * Response DTO pour le test de notification.
     */
    public record SendNotificationTestResponse(
        boolean success,
        String message,
        String idempotencyKey,
        String channel
    ) {}
}

