package com.tchalanet.server.platform.notification.internal.web;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.api.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/platform/tenants/{tenantId}/notifications")
@RequiredArgsConstructor
@Tag(name = "Platform • Tenant notifications")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class PlatformTenantNotificationController {

  private final NotificationAdminGate notificationAdminGate;

  @PostMapping("/announcements")
  public ApiResponse<?> createAnnouncement(
      @PathVariable UUID tenantId, @RequestBody CreateNotificationBody request) {
    notificationAdminGate.createForTenantFromPlatform(request, TenantId.of(tenantId));
    return ApiResponse.created(true);
  }
}
