package com.tchalanet.server.features.notifications.mark_all_notifications_read;

import com.tchalanet.server.common.context.CurrentContext;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.accesscontrol.application.annotation.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Endpoint optionnel pour marquer toutes les notifications de l'utilisateur comme lues. */
@RestController
@RequestMapping("/tenant/me/notifications")
@RequiredArgsConstructor
@Tag(name = "Platform • Notifications")
public class MarkAllNotificationsReadController {

  private final MarkAllNotificationsReadService markAllNotificationsReadService;

  @Operation(summary = "Mark all notifications as read for a user")
  @PostMapping("/read-all")
  @RequiresPermission("notifications:view")
  public ResponseEntity<Void> markAllRead(
      @RequestParam("userId") UserId userId, @CurrentContext TchRequestContext requestContext) {
    var command =
        new MarkAllNotificationsReadCommand(requestContext.tenantId(), userId, Instant.now());
    markAllNotificationsReadService.handle(command);
    return ResponseEntity.noContent().build();
  }
}
