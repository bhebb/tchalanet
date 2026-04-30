package com.tchalanet.server.core.session.infra.web;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchContextResolver;
import com.tchalanet.server.common.types.id.SessionId;
import com.tchalanet.server.core.session.application.command.model.RecomputeSalesSessionTotalsCommand;
import com.tchalanet.server.core.session.application.query.model.GetSessionTotalsQuery;
import com.tchalanet.server.core.session.domain.model.SalesSessionTotals;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tenant/sessions")
@RequiredArgsConstructor
@Tag(name = "Tenant • Sessions")
public class SalesSessionTotalsController {

  private final CommandBus commandBus;
  private final QueryBus queryBus;
  private final TchContextResolver contextResolver;

  /** Read totals for one session (optional endpoint). */
  @Operation(summary = "Get session totals (tenant)")
  @GetMapping("/{sessionId}/totals")
  @PreAuthorize("hasPermission('session.read')")
  public ResponseEntity<SalesSessionTotals> getTotals(@PathVariable SessionId sessionId) {
    var ctx = contextResolver.currentOrNull();
    var tenantId = ctx != null ? ctx.tenantId() : null;

    Optional<SalesSessionTotals> opt = queryBus.send(new GetSessionTotalsQuery(tenantId, sessionId));

    return opt.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
  }

  /** Force recompute totals projection from source of truth (sales/payout or ledger later). */
  @Operation(summary = "Recompute session totals (tenant, restricted)")
  @PostMapping("/{sessionId}/totals/recompute")
  @PreAuthorize("hasPermission('session.totals.recompute')")
  public ResponseEntity<SalesSessionTotals> recompute(@PathVariable SessionId sessionId) {
    var ctx = contextResolver.currentOrNull();
    var tenantId = ctx != null ? ctx.tenantId() : null;

    var totals = commandBus.send(new RecomputeSalesSessionTotalsCommand(tenantId, sessionId));

    return ResponseEntity.ok(totals);
  }
}
