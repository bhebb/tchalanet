package com.tchalanet.server.platform.identity.internal.web.ops;

import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.platform.identity.internal.firebase.FirebaseBootstrapSyncService;
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
public class PlatformIdentitySyncOpsController {

  private final FirebaseBootstrapSyncService syncService;

  @PostMapping("/identity/firebase-bootstrap-users")
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ResponseEntity<ApiResponse<FirebaseBootstrapSyncResponse>> triggerFirebaseBootstrapSync() {
    return runFirebaseBootstrapSync();
  }

  private ResponseEntity<ApiResponse<FirebaseBootstrapSyncResponse>> runFirebaseBootstrapSync() {
    var result = syncService.syncConfiguredUsers();
    var response =
        new FirebaseBootstrapSyncResponse(
            result.attempted(), result.createdInFirebase(), result.linked());
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  public record FirebaseBootstrapSyncResponse(int attempted, int createdInFirebase, int linked) {}
}


