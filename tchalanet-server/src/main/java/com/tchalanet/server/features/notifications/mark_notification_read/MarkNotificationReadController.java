package com.tchalanet.server.features.notifications.mark_notification_read;

import com.tchalanet.server.common.context.CurrentContext;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.core.accesscontrol.application.annotation.RequiresPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Endpoint pour marquer une notification comme lue.
 */
@RestController
@RequestMapping("/api/me/notifications")
@RequiredArgsConstructor
public class MarkNotificationReadController {

    private final MarkNotificationReadService handler;

    @PostMapping("/{id}/read")
    @RequiresPermission("notifications:view")
    public ResponseEntity<Void> markRead(@PathVariable("id") UUID id, @CurrentContext TchRequestContext context) {
        handler.handle(context.userUuid(), id);
        return ResponseEntity.noContent().build();
    }
}

