package com.tchalanet.server.draw.web;

import com.tchalanet.server.draw.domain.usecase.AdminOverrideResultUseCase;
import com.tchalanet.server.draw.domain.usecase.AdminUpdateDrawUseCase;
import com.tchalanet.server.draw.web.dto.OverrideResultRequest;
import com.tchalanet.server.draw.web.dto.UpdateDrawRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/tenants/{tenantId}/draws")
@RequiredArgsConstructor
public class AdminDrawController {

  private final AdminUpdateDrawUseCase adminUpdateDraw;
  private final AdminOverrideResultUseCase adminOverrideResult;

  @PatchMapping("/{drawId}")
  @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('TENANT_ADMIN')")
  public ResponseEntity<?> updateDraw(
      @PathVariable UUID tenantId,
      @PathVariable UUID drawId,
      @RequestBody UpdateDrawRequest req,
      Authentication authentication) {
    UUID adminId = extractUserId(authentication);
    var updated = adminUpdateDraw.updateDraw(tenantId, drawId, req, adminId);
    return ResponseEntity.ok(updated);
  }

  @PostMapping("/{drawId}/override-result")
  @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('TENANT_ADMIN')")
  public ResponseEntity<?> overrideResult(
      @PathVariable UUID tenantId,
      @PathVariable UUID drawId,
      @RequestBody OverrideResultRequest req,
      Authentication authentication) {
    UUID adminId = extractUserId(authentication);
    adminOverrideResult.overrideResult(tenantId, drawId, req, adminId);
    return ResponseEntity.ok().build();
  }

  private UUID extractUserId(Authentication authentication) {
    if (authentication == null) return UUID.fromString("00000000-0000-0000-0000-000000000000");
    Object principal = authentication.getPrincipal();
    if (principal instanceof Jwt jwt) {
      String sub = jwt.getSubject();
      try {
        return UUID.fromString(sub);
      } catch (Exception e) {
        /* fallthrough */
      }
      // support tokens where sub is numeric or not UUID: try claim 'sub' else 'preferred_username'
      Object subClaim = jwt.getClaims().get("sub");
      if (subClaim instanceof String s) {
        try {
          return UUID.fromString(s);
        } catch (Exception ex) {
          /* ignore */
        }
      }
    }
    // fallback: unknown user
    return UUID.fromString("00000000-0000-0000-0000-000000000000");
  }
}
