package com.tchalanet.server.platform.identity.internal.web.ops;

import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.platform.identity.internal.service.KeycloakBootstrapSyncService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/platform/ops/sync")
@Tag(name = "Platform Ops • Sync")
@RequiredArgsConstructor
public class PlatformSyncOpsController {

  private final KeycloakBootstrapSyncService syncService;

  @PostMapping("/identity/keycloak-bootstrap-users")
  @PreAuthorize("hasAuthority('SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<KeycloakBootstrapSyncResponse>> triggerKeycloakBootstrapSync() {
    return runKeycloakBootstrapSync();
  }

  private ResponseEntity<ApiResponse<KeycloakBootstrapSyncResponse>> runKeycloakBootstrapSync() {
    var result = syncService.syncConfiguredUsers();
    var response =
        new KeycloakBootstrapSyncResponse(
            result.attempted(), result.foundInKeycloak(), result.updatedRows());
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  public record KeycloakBootstrapSyncResponse(int attempted, int foundInKeycloak, int updatedRows) {}
}




