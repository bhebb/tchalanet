package com.tchalanet.server.core.notification.infra.web;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.CurrentContext;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.types.id.NotificationId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.common.web.paging.TchPaging;
import com.tchalanet.server.core.notification.application.command.model.ArchiveNotificationCommand;
import com.tchalanet.server.core.notification.application.command.model.ArchiveNotificationsCommand;
import com.tchalanet.server.core.notification.application.command.model.MarkNotificationReadCommand;
import com.tchalanet.server.core.notification.application.command.model.MarkNotificationsReadCommand;
import com.tchalanet.server.core.notification.application.query.model.GetNotificationSummaryQuery;
import com.tchalanet.server.core.notification.application.query.model.ListNotificationsQuery;
import com.tchalanet.server.core.notification.domain.model.NotificationCategory;
import com.tchalanet.server.core.notification.domain.model.NotificationKind;
import com.tchalanet.server.core.notification.domain.model.NotificationSeverity;
import com.tchalanet.server.core.notification.domain.model.NotificationStatus;
import com.tchalanet.server.core.notification.infra.web.dto.NotificationBulkActionRequest;
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
@RequestMapping("/tenant/notifications")
@RequiredArgsConstructor
@Tag(name = "Tenant • Notifications")
@PreAuthorize("hasPermission('notifications:view')")
public class TenantNotificationController {

  private final CommandBus commandBus;
  private final QueryBus queryBus;

  @GetMapping("/summary")
  public ApiResponse<?> summary(@CurrentContext TchRequestContext context) {
    return ApiResponse.success(
        queryBus.ask(new GetNotificationSummaryQuery(context.userId(), roleCode(context))));
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
        queryBus.ask(
            new ListNotificationsQuery(
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
    commandBus.execute(new MarkNotificationReadCommand(id, context.userId()));
    return ApiResponse.success(true);
  }

  @PostMapping("/read")
  public ApiResponse<?> markRead(
      @Valid @RequestBody NotificationBulkActionRequest request,
      @CurrentContext TchRequestContext context) {
    commandBus.execute(
        new MarkNotificationsReadCommand(
            request.ids().stream().map(NotificationId::of).toList(), context.userId()));
    return ApiResponse.success(true);
  }

  @PostMapping("/{id}/archive")
  public ApiResponse<?> archive(
      @PathVariable NotificationId id, @CurrentContext TchRequestContext context) {
    commandBus.execute(new ArchiveNotificationCommand(id, context.userId()));
    return ApiResponse.success(true);
  }

  @PostMapping("/archive")
  public ApiResponse<?> archive(
      @Valid @RequestBody NotificationBulkActionRequest request,
      @CurrentContext TchRequestContext context) {
    commandBus.execute(
        new ArchiveNotificationsCommand(
            request.ids().stream().map(NotificationId::of).toList(), context.userId()));
    return ApiResponse.success(true);
  }

  private static String roleCode(TchRequestContext context) {
    return context.currentRole() == null ? null : context.currentRole().name();
  }
}
