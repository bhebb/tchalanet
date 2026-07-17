package com.tchalanet.server.features.ops.sales;

import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.platform.audit.api.AuditLog;
import com.tchalanet.server.platform.audit.api.model.AuditAction;
import com.tchalanet.server.platform.audit.api.model.AuditEntityType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/platform/ops/sales-simulations")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Tag(name = "Ops • Sales simulation")
public class OpsSalesSimulationController {

  private final OpsSalesSimulationService service;

  @Operation(summary = "Plan or execute deterministic sales across selected draws and seller terminals")
  @PostMapping
  @AuditLog(
      entity = AuditEntityType.TICKET,
      action = AuditAction.TICKET_SELL,
      idExpression = "#req.tenantId().toString()",
      detailsExpression = "#req")
  public ApiResponse<OpsSalesSimulationResponse> simulate(
      @Valid @RequestBody OpsSalesSimulationRequest req) {
    return ApiResponse.success(service.simulate(req));
  }
}
