package com.tchalanet.server.core.outlet.internal.infra.web.tenant;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.core.outlet.api.command.block.OutletOperationalControl;
import com.tchalanet.server.core.outlet.api.command.block.SetOutletOperationalControlCommand;
import com.tchalanet.server.core.outlet.api.command.lifecycle.SetOutletStatusCommand;
import com.tchalanet.server.core.outlet.internal.domain.model.OutletStatus;
import com.tchalanet.server.core.outlet.internal.infra.web.admin.model.SetOperationalControlRequest;
import com.tchalanet.server.core.outlet.internal.infra.web.admin.model.SetOutletStatusRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/outlets/{outletId}/operational-controls")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'SUPER_ADMIN')")
public class AdminOutletOperationalControlsController {

  private final CommandBus commandBus;

  @PatchMapping("/status")
  public ApiResponse<Void> setStatus(
      @CurrentContext TchRequestContext ctx,
      @PathVariable OutletId outletId,
      @Valid @RequestBody SetOutletStatusRequest request) {
    commandBus.execute(new SetOutletStatusCommand(
        outletId,
        OutletStatus.valueOf(request.status()),
        request.reason(),
        ctx.currentUserIdRequired()));
    return ApiResponse.success(null);
  }

  @PatchMapping("/{control}")
  public ApiResponse<Void> setControl(
      @CurrentContext TchRequestContext ctx,
      @PathVariable OutletId outletId,
      @PathVariable OutletOperationalControl control,
      @Valid @RequestBody SetOperationalControlRequest request) {
    commandBus.execute(new SetOutletOperationalControlCommand(
        outletId,
        control,
        request.blocked(),
        request.reason(),
        ctx.currentUserIdRequired()));
    return ApiResponse.success(null);
  }
}
