package com.tchalanet.server.core.session.infra.web;
import com.tchalanet.server.common.types.id.SessionId;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContextHolder;
import com.tchalanet.server.core.accesscontrol.application.annotation.RequiresPermission;
import com.tchalanet.server.core.session.application.command.model.RecomputePosSessionTotalsCommand;
import com.tchalanet.server.core.session.application.query.model.GetSessionTotalsQuery;
import com.tchalanet.server.core.session.domain.model.PosSessionTotals;

import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sessions")
@RequiredArgsConstructor
public class PosSessionTotalsController {

    private final CommandBus commandBus;
    private final QueryBus queryBus;
    private final TchRequestContextHolder ctxHolder;

    /**
     * Read totals for one session (optional endpoint).
     */
    @GetMapping("/{sessionId}/totals")
    @RequiresPermission("session.read")
    public ResponseEntity<PosSessionTotals> getTotals(@PathVariable SessionId sessionId) {
        var tenantId = ctxHolder.get().tenantUuid();

        Optional<PosSessionTotals> opt = queryBus.send(new GetSessionTotalsQuery(tenantId, sessionId));

        return opt.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    /**
     * Force recompute totals projection from source of truth (sales/payout or ledger later).
     */
    @PostMapping("/{sessionId}/totals/recompute")
    @RequiresPermission("session.totals.recompute") // admin/manager only
    public ResponseEntity<PosSessionTotals> recompute(@PathVariable SessionId sessionId) {
        var tenantId = ctxHolder.get().tenantUuid();

        var totals = commandBus.send(new RecomputePosSessionTotalsCommand(tenantId, sessionId));

        return ResponseEntity.ok(totals);
    }
}
