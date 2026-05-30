package com.tchalanet.server.core.outlet.internal.infra.web.tenant;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.outlet.api.command.lifecycle.CloseOutletDayCommand;
import com.tchalanet.server.core.outlet.api.command.lifecycle.CloseOutletDayPayload;
import com.tchalanet.server.core.outlet.internal.infra.web.tenant.model.CloseCurrentOutletDayRequest;
import com.tchalanet.server.platform.accesscontrol.api.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Seller/responsable operational endpoint: close the CURRENT outlet for the day.
 *
 * <p>The outlet is resolved from the <b>trusted</b> operational context (POS
 * frame), never from a path/body — a seller can only act on the outlet they are
 * operating. Gated by the {@code outlet.day.close} permission. Reuses the same
 * {@link CloseOutletDayCommand} as the admin path (validates open sessions, sets
 * {@code outlet.day_closed}, publishes {@code OutletDayClosedEvent}).
 */
@RestController
@RequestMapping("/tenant/outlet/current")
@PreAuthorize("hasAnyAuthority('CASHIER','TENANT_ADMIN','SUPER_ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Outlet • Current (operational)")
public class CurrentOutletDayController {

  private final CommandBus commandBus;

  @PostMapping("/close-day")
  @Operation(summary = "Close the current outlet (from trusted operational context) for today")
  @RequiresPermission("outlet.day.close")
  public ApiResponse<Void> closeCurrentOutletDay(
      @CurrentContext TchRequestContext ctx,
      @Valid @RequestBody(required = false) CloseCurrentOutletDayRequest request) {

    var operational = ctx.trustedOperationalContextRequired();
    var outletId = operational.outletId();
    if (outletId == null) {
      throw ProblemRest.unprocessable("operational_context.outlet_required");
    }

    var req = request == null ? new CloseCurrentOutletDayRequest(null, null) : request;
    // null from/to → the handler normalizes to "today" using its injected Clock.
    var payload = new CloseOutletDayPayload(null, null, req.modeOrDefault(), req.reason());

    commandBus.execute(new CloseOutletDayCommand(
        ctx.effectiveTenantIdRequired(), outletId, payload, ctx.currentUserIdRequired()));
    return ApiResponse.success(null);
  }
}
