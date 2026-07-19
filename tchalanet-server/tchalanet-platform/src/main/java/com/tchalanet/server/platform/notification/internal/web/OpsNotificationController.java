package com.tchalanet.server.platform.notification.internal.web;

import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.platform.notification.api.model.NotificationChannel;
import com.tchalanet.server.platform.notification.api.model.NotificationRecipient;
import com.tchalanet.server.platform.notification.api.model.NotificationSeverity;
import com.tchalanet.server.platform.notification.api.model.NotificationType;
import com.tchalanet.server.platform.notification.api.model.request.SendNotificationRequest;
import com.tchalanet.server.platform.notification.internal.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/platform/ops/notifications")
@Tag(name = "Ops • Notifications")
@RequiredArgsConstructor
@Slf4j
public class OpsNotificationController {

  private final NotificationService notificationService;

  @Operation(
      summary = "Create a test in-app notification (SUPER_ADMIN only)",
      description = "Creates a persistent notification-center item for ops verification.")
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  @PostMapping("/test")
  public ResponseEntity<ApiResponse<SendNotificationTestResponse>> testNotification(
      @Valid @RequestBody SendNotificationTestRequest request) {
    log.info(
        "Ops notification test request: channel={}, severity={}",
        request.channel,
        request.severity);

    var recipient =
        new NotificationRecipient(
            request.channel,
            request.to,
            request.channelKey,
            null, // no tenant for ops test
            null // no user for ops test
            );

    var command =
        new SendNotificationRequest(
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

    var result = notificationService.sendNotification(command);
    var response =
        new SendNotificationTestResponse(
            result.success(), result.code(), result.idempotencyKey(), request.channel.name());
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  /** Request DTO pour tester la création de notifications in-app. */
  public record SendNotificationTestRequest(
      @NotNull NotificationChannel channel,
      @Nullable String to,
      @Nullable String channelKey,
      @NotNull NotificationSeverity severity,
      @NotBlank String title,
      @NotBlank String message,
      @Nullable Map<String, Object> context) {}

  /** Response DTO pour le test de notification. */
  public record SendNotificationTestResponse(
      boolean success, String code, String idempotencyKey, String channel) {}
}
