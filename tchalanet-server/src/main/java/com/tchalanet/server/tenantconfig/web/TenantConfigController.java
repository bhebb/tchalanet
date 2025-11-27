package com.tchalanet.server.tenantconfig.web;

import com.tchalanet.server.accesscontrol.application.annotation.RequiresPermission; // New import
import com.tchalanet.server.tenantconfig.domain.ports.in.UpsertTenantConfigUseCase;
import com.tchalanet.server.tenantconfig.web.dto.TenantSettingRequest;
import com.tchalanet.server.tenantconfig.web.dto.TenantSettingResponse;
import com.tchalanet.server.tenantconfig.web.mapper.TenantConfigWebMapper;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/config")
@RequiredArgsConstructor
public class TenantConfigController {

  private final UpsertTenantConfigUseCase upsertTenantConfigUseCase;
  private final TenantConfigWebMapper mapper;

  // private final AccessCheckerPort accessChecker; // No longer directly injected here, handled by
  // annotation

  @PostMapping
  @RequiresPermission("tenant_config.manage") // Apply the annotation
  public ResponseEntity<TenantSettingResponse> upsertTenantSetting(
      @PathVariable UUID tenantId,
      @RequestHeader("X-User-Id")
          UUID userId, // Assuming userId is passed in header for admin actions
      @Valid @RequestBody TenantSettingRequest request) {
    var command = mapper.toUpsertCommand(tenantId, request);
    var setting = upsertTenantConfigUseCase.upsert(command);
    return new ResponseEntity<>(mapper.toTenantSettingResponse(setting), HttpStatus.CREATED);
  }

  @GetMapping("/{settingId}")
  @RequiresPermission("tenant_config.view") // Apply the annotation
  public ResponseEntity<TenantSettingResponse> getTenantSetting(
      @PathVariable UUID tenantId,
      @RequestHeader("X-User-Id") UUID userId,
      @PathVariable UUID settingId) {
    var setting =
        upsertTenantConfigUseCase
            .getTenantSetting(settingId)
            .filter(s -> s.getTenantId().equals(tenantId)) // Security check
            .map(mapper::toTenantSettingResponse)
            .orElseThrow(
                () -> new TenantSettingNotFoundException("Tenant Setting not found: " + settingId));
    return ResponseEntity.ok(setting);
  }

  @ResponseStatus(HttpStatus.NOT_FOUND)
  public static class TenantSettingNotFoundException extends RuntimeException {
    public TenantSettingNotFoundException(String message) {
      super(message);
    }
  }
}
