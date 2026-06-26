package com.tchalanet.server.platform.notification.internal.web;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.types.id.NotificationId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.common.web.paging.TchPaging;
import com.tchalanet.server.common.web.paging.TchSearchQuery;
import com.tchalanet.server.platform.notification.api.model.NotificationCategory;
import com.tchalanet.server.platform.notification.api.model.NotificationKind;
import com.tchalanet.server.platform.notification.api.model.NotificationSeverity;
import com.tchalanet.server.platform.notification.api.model.NotificationStatus;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/platform/notifications")
@RequiredArgsConstructor
@Tag(name = "Platform • Notifications")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class PlatformNotificationController {

  private final NotificationAdminGate notificationAdminGate;

  @GetMapping("/summary")
  public ApiResponse<?> summary(@CurrentContext TchRequestContext context) {
    return ApiResponse.success(notificationAdminGate.summary(context));
  }

  @GetMapping
  public ApiResponse<?> list(
      @RequestParam(required = false, name = "q") String q,
      @RequestParam(required = false) NotificationStatus status,
      @RequestParam(required = false) NotificationCategory category,
      @RequestParam(required = false) NotificationKind kind,
      @RequestParam(required = false) NotificationSeverity severity,
      @TchPaging TchPageRequest pageRequest,
      @CurrentContext TchRequestContext context) {
    return ApiResponse.success(
        notificationAdminGate.listMy(
            status, category, kind, severity, TchSearchQuery.of(q), pageRequest, context));
  }

  @GetMapping("/unread-count")
  public ApiResponse<?> unreadCount(@CurrentContext TchRequestContext context) {
    return ApiResponse.success(notificationAdminGate.unreadCount(context));
  }

  @PostMapping
  public ApiResponse<?> create(@RequestBody CreateNotificationBody request) {
    notificationAdminGate.createForPlatform(request);
    return ApiResponse.created(true);
  }

  @PostMapping("/{id}/read")
  public ApiResponse<?> markRead(
      @PathVariable NotificationId id, @CurrentContext TchRequestContext context) {
    notificationAdminGate.markRead(id, context);
    return ApiResponse.success(true);
  }

  @PostMapping("/{id}/archive")
  public ApiResponse<?> archive(
      @PathVariable NotificationId id, @CurrentContext TchRequestContext context) {
    notificationAdminGate.archive(id, context);
    return ApiResponse.success(true);
  }

  @PostMapping("/{id}/dismiss")
  public ApiResponse<?> dismiss(
      @PathVariable NotificationId id, @CurrentContext TchRequestContext context) {
    notificationAdminGate.dismiss(id, context);
    return ApiResponse.success(true);
  }

  @PostMapping("/read-all")
  public ApiResponse<?> markAllRead(@CurrentContext TchRequestContext context) {
    notificationAdminGate.markAllRead(context);
    return ApiResponse.success(true);
  }
}
