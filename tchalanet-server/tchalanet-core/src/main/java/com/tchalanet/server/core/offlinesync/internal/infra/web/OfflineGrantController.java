package com.tchalanet.server.core.offlinesync.internal.infra.web;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.types.id.OfflineCodeBatchId;
import com.tchalanet.server.common.types.id.OfflineSalesGrantId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.core.offlinesync.api.command.IssueOfflineSalesGrantCommand;
import com.tchalanet.server.core.offlinesync.api.command.RevokeOfflineSalesGrantCommand;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tenant/offline-sync/grants")
@PreAuthorize("hasAnyAuthority('CASHIER', 'TENANT_ADMIN', 'SUPER_ADMIN')")
public class OfflineGrantController {

  private final CommandBus commandBus;

  public OfflineGrantController(CommandBus commandBus) {
    this.commandBus = commandBus;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.ACCEPTED)
  public ApiResponse<Map<String, String>> issue(
      @CurrentContext TchRequestContext ctx,
      @RequestBody IssueGrantRequest request) {
    var result = commandBus.execute(new IssueOfflineSalesGrantCommand(
        ctx.effectiveTenantIdRequired(),
        ctx.terminalIdRequired(),
        ctx.outletIdRequired(),
        ctx.salesSessionIdRequired(),
        ctx.currentUserIdRequired(),
        OfflineCodeBatchId.parse(request.codeBatchId()),
        request.expiresAt()));
    return ApiResponse.success(Map.of("grantId", result.grantId().value().toString(), "status", result.status().name()));
  }

  @DeleteMapping("/{grantId}")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public ApiResponse<Map<String, String>> revoke(
      @CurrentContext TchRequestContext ctx,
      @PathVariable String grantId,
      @RequestBody RevokeGrantRequest request) {
    var result = commandBus.execute(new RevokeOfflineSalesGrantCommand(
        ctx.effectiveTenantIdRequired(),
        OfflineSalesGrantId.parse(grantId),
        ctx.currentUserIdRequired(),
        request.reason()));
    return ApiResponse.success(Map.of("grantId", result.grantId().value().toString()));
  }

  public record IssueGrantRequest(String codeBatchId, Instant expiresAt) {}

  public record RevokeGrantRequest(String reason) {}
}

