package com.tchalanet.server.core.notification.infra.web;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.CurrentContext;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.types.id.NotificationId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.common.web.paging.TchPaging;
import com.tchalanet.server.core.notification.application.command.model.CreateNotificationCommand;
import com.tchalanet.server.core.notification.application.query.model.ListNotificationDeliveriesQuery;
import com.tchalanet.server.core.notification.application.query.model.ListNotificationsQuery;
import com.tchalanet.server.core.notification.domain.model.NotificationAudienceType;
import com.tchalanet.server.core.notification.domain.model.NotificationCategory;
import com.tchalanet.server.core.notification.domain.model.NotificationDeliveryStatus;
import com.tchalanet.server.core.notification.domain.model.NotificationKind;
import com.tchalanet.server.core.notification.domain.model.NotificationSeverity;
import com.tchalanet.server.core.notification.domain.model.NotificationStatus;
import com.tchalanet.server.core.notification.infra.web.dto.CreateNotificationRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/notifications")
@RequiredArgsConstructor
@Tag(name = "Admin • Notifications")
@PreAuthorize("hasAuthority('TENANT_ADMIN') or hasAuthority('SUPER_ADMIN')")
public class AdminNotificationController {

  private final CommandBus commandBus;
  private final QueryBus queryBus;

  @GetMapping
  public ApiResponse<?> list(
      @RequestParam(required = false) NotificationStatus status,
      @RequestParam(required = false) NotificationCategory category,
      @RequestParam(required = false) NotificationKind kind,
      @RequestParam(required = false) NotificationSeverity severity,
      @TchPaging TchPageRequest pageRequest,
      @CurrentContext TchRequestContext context) {
    return ApiResponse.success(
        queryBus.ask(
            new ListNotificationsQuery(
                context.userId(),
                context.currentRole() == null ? null : context.currentRole().name(),
                Optional.ofNullable(status),
                Optional.ofNullable(category),
                Optional.ofNullable(kind),
                Optional.ofNullable(severity),
                pageRequest)));
  }

  @PostMapping
  public ApiResponse<?> create(
      @RequestBody CreateNotificationRequest request, @CurrentContext TchRequestContext context) {
    var audienceType =
        request.audienceType() == null ? NotificationAudienceType.TENANT : request.audienceType();
    commandBus.execute(
        new CreateNotificationCommand(
            context.tenantId(),
            request.sourceType(),
            request.sourceId(),
            request.dedupeKey(),
            audienceType,
            request.audienceValue(),
            request.severity(),
            request.kind(),
            request.category(),
            request.titleKey(),
            request.messageKey(),
            request.titleText(),
            request.messageText(),
            request.payload(),
            request.actionType(),
            request.actionUrl(),
            request.expiresAt(),
            request.channels()));
    return ApiResponse.created(true);
  }

  @GetMapping("/deliveries")
  public ApiResponse<?> deliveries(
      @RequestParam(required = false) NotificationId notificationId,
      @RequestParam(required = false) NotificationDeliveryStatus status,
      @TchPaging TchPageRequest pageRequest) {
    return ApiResponse.success(
        queryBus.ask(
            new ListNotificationDeliveriesQuery(
                Optional.ofNullable(notificationId), Optional.ofNullable(status), pageRequest)));
  }
}
