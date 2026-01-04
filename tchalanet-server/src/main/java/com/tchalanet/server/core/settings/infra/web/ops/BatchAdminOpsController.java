package com.tchalanet.server.common.settings.web;

import com.tchalanet.server.common.persistence.AppSettingEntity;
import com.tchalanet.server.common.persistence.AppSettingRepository;
import com.tchalanet.server.common.settings.AppSettingLevel;
import com.tchalanet.server.common.settings.AppSettingValueType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/platform/ops/batch")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
@Tag(name = "Ops • Batch Admin")
public class BatchAdminOpsController {

  private static final String NS = "batch";

  private final AppSettingRepository repo;

  @Operation(summary = "Set batch flag (GLOBAL)")
  @PostMapping("/global-flag")
  public AppSettingEntity setGlobal(@RequestBody SetFlagRequest req) {
    return upsert(AppSettingLevel.GLOBAL, null, req.key(), req.enabled());
  }

  @Operation(summary = "Set batch flag (TENANT)")
  @PostMapping("/tenant-flag")
  public AppSettingEntity setTenant(@RequestBody SetTenantFlagRequest req) {
    return upsert(AppSettingLevel.TENANT, req.tenantId(), req.key(), req.enabled());
  }

  private AppSettingEntity upsert(AppSettingLevel level, UUID tenantId, String key, boolean enabled) {
    var existing =
        repo.findFirstByActiveTrueAndDeletedAtIsNullAndLevelAndTenantIdAndOutletIdAndTerminalIdAndNamespaceAndSettingKey(
            level, tenantId, null, null, NS, key);

    AppSettingEntity e = existing.orElseGet(AppSettingEntity::new);
    e.setLevel(level);
    e.setTenantId(tenantId);
    e.setOutletId(null);
    e.setTerminalId(null);
    e.setNamespace(NS);
    e.setSettingKey(key);
    e.setValueType(AppSettingValueType.BOOLEAN);
    e.setSettingValue(Boolean.toString(enabled));
    e.setActive(true);

    return repo.save(e); // listener evict cache => effect immediate
  }

  public record SetFlagRequest(String key, boolean enabled) {}
  public record SetTenantFlagRequest(UUID tenantId, String key, boolean enabled) {}
}

