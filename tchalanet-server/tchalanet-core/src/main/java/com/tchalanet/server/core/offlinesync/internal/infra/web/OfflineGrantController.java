package com.tchalanet.server.core.offlinesync.internal.infra.web;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.id.OfflineCodeBatchId;
import com.tchalanet.server.common.types.id.OfflineSalesGrantId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.offlinesync.api.command.IssueOfflineSalesGrantCommand;
import com.tchalanet.server.core.offlinesync.api.command.RevokeOfflineSalesGrantCommand;
import com.tchalanet.server.core.offlinesync.api.query.GetOfflineGrantQuery;
import com.tchalanet.server.core.offlinesync.internal.domain.model.OfflineSalesGrant;
import com.tchalanet.server.core.offlinesync.internal.domain.model.OfflineSalesGrantStatus;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
  private final QueryBus queryBus;

  public OfflineGrantController(CommandBus commandBus, QueryBus queryBus) {
    this.commandBus = commandBus;
    this.queryBus = queryBus;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.ACCEPTED)
  public ApiResponse<Map<String, String>> issue(
      @CurrentContext TchRequestContext ctx,
      @RequestBody IssueGrantRequest request) {
    var result = commandBus.execute(new IssueOfflineSalesGrantCommand(
        ctx.effectiveTenantIdRequired(),
        ctx.currentUserIdRequired(),
        ctx.operationalContextRequired(),
        OfflineCodeBatchId.parse(request.codeBatchId()),
        request.expiresAt()));
    return ApiResponse.success(Map.of("grantId", result.grantId().value().toString(), "status", result.status().name()));
  }

  @GetMapping("/{grantId}")
  public ApiResponse<OfflineGrantResponse> get(
      @CurrentContext TchRequestContext ctx,
      @PathVariable String grantId) {
    var grant = queryBus.ask(new GetOfflineGrantQuery(
        ctx.effectiveTenantIdRequired(),
        OfflineSalesGrantId.parse(grantId)));
    if (grant == null) {
      throw ProblemRest.notFound("offline_grant.not_found", grantId);
    }
    return ApiResponse.success(toResponse(grant));
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

  public record OfflineGrantResponse(
      String grantId,
      String tenantId,
      String terminalId,
      String outletId,
      String salesSessionId,
      String sellerUserId,
      String codeBatchId,
      OfflineSalesGrantStatus status,
      Instant issuedAt,
      Instant expiresAt
  ) {}

  private OfflineGrantResponse toResponse(OfflineSalesGrant grant) {
    return new OfflineGrantResponse(
        grant.id().value().toString(),
        grant.tenantId().value().toString(),
        grant.terminalId().value().toString(),
        grant.outletId().value().toString(),
        grant.salesSessionId().value().toString(),
        grant.sellerUserId().value().toString(),
        grant.codeBatchId().value().toString(),
        grant.status(),
        grant.issuedAt(),
        grant.expiresAt());
  }
}
