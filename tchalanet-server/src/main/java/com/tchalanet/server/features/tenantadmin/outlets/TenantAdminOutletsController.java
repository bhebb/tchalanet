package com.tchalanet.server.features.tenantadmin.outlets;

import com.tchalanet.server.common.context.CurrentContext;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.features.tenantadmin.outlets.model.OutletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
@RestController
@RequestMapping("/admin/outlets")
@PreAuthorize("hasAnyRole('TENANT_ADMIN','SUPER_ADMIN')")
@RequiredArgsConstructor
public class TenantAdminOutletsController {

  private final TenantAdminOutletsOrchestrator orchestrator;

  @GetMapping
  public ApiResponse<List<OutletResponse>> list(@CurrentContext TchRequestContext ctx) {
    return ApiResponse.success(orchestrator.list(ctx));
  }

  @GetMapping("/{id}")
  public ApiResponse<OutletResponse> get(@CurrentContext TchRequestContext ctx, @PathVariable OutletId id) {
    return ApiResponse.success(orchestrator.get(ctx, id));
  }

  public record CreateOutletReq(String name, String slug, com.tchalanet.server.core.address.application.model.AddressInput address) {}

  @PostMapping
  public ApiResponse<OutletId> create(@CurrentContext TchRequestContext ctx, @Valid @RequestBody CreateOutletReq req) {
    var id = orchestrator.create(ctx, req.name, req.slug, req.address);
    return ApiResponse.success(id);
  }

  @PatchMapping("/{id}/config")
  public ApiResponse<Void> updateConfig(@CurrentContext TchRequestContext ctx, @PathVariable OutletId id, @RequestBody com.tchalanet.server.core.outlet.application.command.model.OutletConfigPatch patch) {
    orchestrator.updateConfig(ctx, id, patch);
    return ApiResponse.success(null);
  }
}
