package com.tchalanet.server.platform.notification.internal.web;

import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.types.id.NotificationId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.common.web.paging.TchPaging;
import com.tchalanet.server.platform.notification.api.model.request.ArchiveNotificationRequest;
import com.tchalanet.server.platform.notification.api.model.request.ArchiveNotificationsRequest;
import com.tchalanet.server.platform.notification.api.model.request.MarkNotificationReadRequest;
import com.tchalanet.server.platform.notification.api.model.request.MarkNotificationsReadRequest;
import com.tchalanet.server.platform.notification.api.model.request.GetNotificationSummaryRequest;
import com.tchalanet.server.platform.notification.api.model.request.ListNotificationsRequest;
import com.tchalanet.server.platform.notification.api.model.NotificationCategory;
import com.tchalanet.server.platform.notification.api.model.NotificationKind;
import com.tchalanet.server.platform.notification.api.model.NotificationSeverity;
import com.tchalanet.server.platform.notification.api.model.NotificationStatus;
import com.tchalanet.server.platform.notification.internal.service.NotificationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tenant/me/notifications")
@RequiredArgsConstructor
@Tag(name = "Tenant • Notifications")
@PreAuthorize("hasPermission('notifications:view')")
public class TenantNotificationController {

  private final NotificationService notificationService;

  @GetMapping("/summary")
  public ApiResponse<?> summary(@CurrentContext TchRequestContext context) {
    return ApiResponse.success(
        notificationService.getNotificationSummary(
            new GetNotificationSummaryRequest(context.userId(), roleCode(context))));
  }

  @GetMapping
  public ApiResponse<?> list(
      @RequestParam(required = false) NotificationStatus status,
      @RequestParam(required = false) NotificationCategory category,
      @RequestParam(required = false) NotificationKind kind,
      @RequestParam(required = false) NotificationSeverity severity,
      @TchPaging TchPageRequest pageRequest,
      @CurrentContext TchRequestContext context) {
    return ApiResponse.success(
        notificationService.listNotifications(
            new ListNotificationsRequest(
                context.userId(),
                roleCode(context),
                Optional.ofNullable(status),
                Optional.ofNullable(category),
                Optional.ofNullable(kind),
                Optional.ofNullable(severity),
                pageRequest)));
  }

  @PostMapping("/{id}/read")
  public ApiResponse<?> markRead(
      @PathVariable NotificationId id, @CurrentContext TchRequestContext context) {
    notificationService.markRead(new MarkNotificationReadRequest(id, context.userId()));
    return ApiResponse.success(true);
  }

  @PostMapping("/read")
  public ApiResponse<?> markRead(
      @Valid @RequestBody NotificationBulkActionRequest request,
      @CurrentContext TchRequestContext context) {
    notificationService.markRead(
        new MarkNotificationsReadRequest(
            request.ids().stream().map(NotificationId::of).toList(), context.userId()));
    return ApiResponse.success(true);
  }

  @PostMapping("/{id}/archive")
  public ApiResponse<?> archive(
      @PathVariable NotificationId id, @CurrentContext TchRequestContext context) {
    notificationService.archiveNotification(new ArchiveNotificationRequest(id, context.userId()));
    return ApiResponse.success(true);
  }

  @PostMapping("/archive")
  public ApiResponse<?> archive(
      @Valid @RequestBody NotificationBulkActionRequest request,
      @CurrentContext TchRequestContext context) {
    notificationService.archiveNotifications(
        new ArchiveNotificationsRequest(
            request.ids().stream().map(NotificationId::of).toList(), context.userId()));
    return ApiResponse.success(true);
  }

  private static String roleCode(TchRequestContext context) {
    return context.currentRole() == null ? null : context.currentRole().name();
  }
}
