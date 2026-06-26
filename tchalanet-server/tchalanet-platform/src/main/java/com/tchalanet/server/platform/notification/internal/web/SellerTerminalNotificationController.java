package com.tchalanet.server.platform.notification.internal.web;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.types.id.NotificationId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.common.web.paging.TchPaging;
import com.tchalanet.server.common.web.paging.TchSearchQuery;
import com.tchalanet.server.platform.notification.api.model.NotificationCategory;
import com.tchalanet.server.platform.notification.api.model.NotificationActorType;
import com.tchalanet.server.platform.notification.api.model.NotificationKind;
import com.tchalanet.server.platform.notification.api.model.NotificationSeverity;
import com.tchalanet.server.platform.notification.api.model.NotificationStatus;
import com.tchalanet.server.platform.notification.internal.service.NotificationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tenant/seller-terminal/me/notifications")
@RequiredArgsConstructor
@Tag(name = "Seller Terminal • Notifications")
@PreAuthorize("hasAuthority('ACTOR_SELLER_TERMINAL')")
public class SellerTerminalNotificationController {

  private final NotificationService notificationService;

  @GetMapping("/summary")
  public ApiResponse<?> summary(@CurrentContext TchRequestContext context) {
    return ApiResponse.success(
        notificationService.getTerminalNotificationSummary(context.sellerTerminalIdRequired()));
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
        notificationService.listMyNotifications(
            NotificationActorType.SELLER_TERMINAL,
            context.sellerTerminalIdRequired().value(),
            null,
            null,
            Optional.ofNullable(status),
            Optional.ofNullable(category),
            Optional.ofNullable(kind),
            Optional.ofNullable(severity),
            TchSearchQuery.of(q),
            pageRequest));
  }

  @GetMapping("/unread-count")
  public ApiResponse<?> unreadCount(@CurrentContext TchRequestContext context) {
    return ApiResponse.success(
        notificationService.countUnread(
            NotificationActorType.SELLER_TERMINAL,
            context.sellerTerminalIdRequired().value(),
            null,
            null));
  }

  @PostMapping("/{id}/read")
  public ApiResponse<?> markRead(
      @PathVariable NotificationId id, @CurrentContext TchRequestContext context) {
    notificationService.markRead(
        id, NotificationActorType.SELLER_TERMINAL, context.sellerTerminalIdRequired().value());
    return ApiResponse.success(true);
  }

  @PostMapping("/{id}/archive")
  public ApiResponse<?> archive(
      @PathVariable NotificationId id, @CurrentContext TchRequestContext context) {
    notificationService.archiveNotificationForTerminal(id, context.sellerTerminalIdRequired());
    return ApiResponse.success(true);
  }

  @PostMapping("/{id}/dismiss")
  public ApiResponse<?> dismiss(
      @PathVariable NotificationId id, @CurrentContext TchRequestContext context) {
    notificationService.dismiss(
        id, NotificationActorType.SELLER_TERMINAL, context.sellerTerminalIdRequired().value());
    return ApiResponse.success(true);
  }

  @PostMapping("/read-all")
  public ApiResponse<?> markAllRead(@CurrentContext TchRequestContext context) {
    notificationService.markAllRead(
        NotificationActorType.SELLER_TERMINAL,
        context.sellerTerminalIdRequired().value(),
        null,
        null);
    return ApiResponse.success(true);
  }
}
