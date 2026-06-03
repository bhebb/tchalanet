package com.tchalanet.server.features.cashier.operationalcontext.app;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.core.outlet.api.query.GetOutletByIdQuery;
import com.tchalanet.server.core.outlet.api.query.ListOutletsByTenantQuery;
import com.tchalanet.server.core.outlet.api.query.ListOutletTerminalsQuery;
import com.tchalanet.server.core.outlet.api.query.OutletOperation;
import com.tchalanet.server.core.outlet.api.query.OutletView;
import com.tchalanet.server.core.outlet.api.query.OutletTerminalView;
import com.tchalanet.server.core.outlet.api.query.ValidateOutletForOperationQuery;
import com.tchalanet.server.core.session.api.query.SalesSessionOperation;
import com.tchalanet.server.core.session.api.query.ValidateSalesSessionForOperationQuery;
import com.tchalanet.server.core.terminal.api.query.ListTerminalsQuery;
import com.tchalanet.server.core.terminal.api.query.TerminalOperation;
import com.tchalanet.server.core.terminal.api.query.TerminalSearchCriteria;
import com.tchalanet.server.core.terminal.api.query.TerminalSummaryView;
import com.tchalanet.server.core.terminal.api.query.ValidateTerminalForOperationQuery;
import com.tchalanet.server.features.cashier.operationalcontext.model.CashierOpContextOptionsView;
import com.tchalanet.server.features.cashier.operationalcontext.model.CashierOpContextOptionsView.DefaultSelection;
import com.tchalanet.server.features.cashier.operationalcontext.model.CashierOpContextOptionsView.OutletOption;
import com.tchalanet.server.features.cashier.operationalcontext.model.CashierOpContextOptionsView.TerminalOption;
import com.tchalanet.server.features.cashier.operationalcontext.model.CashierOperationalContextView;
import com.tchalanet.server.features.cashier.operationalcontext.model.SelectCashierOperationalContextRequest;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

/**
 * Cashier-facing operational context BFF.
 *
 * <p>The seller's selected (outlet, terminal, session) frame is currently transported per
 * request via {@code X-Tch-*} headers and lives in {@link TchRequestContext#operationalContext()}.
 * This service therefore:
 *
 * <ul>
 *   <li>{@code current()} — projects the headers attached to the current request;
 *   <li>{@code select(...)} — validates that the seller can use the requested outlet/terminal/session
 *       and echoes the validated view back. Persistence of the selection is the client's
 *       responsibility (it must keep sending the matching headers).
 *   <li>{@code clear()} — no-op server-side. The client stops sending the headers.
 * </ul>
 *
 * <p>TODO: when server-side selection persistence (per-user store, cookie, or JWT claim) is
 * introduced, wire it here.
 */
@Service
@RequiredArgsConstructor
public class CashierOperationalContextService {

    private final QueryBus queryBus;

    public CashierOperationalContextView current(TchRequestContext ctx) {
        return CashierOperationalContextView.from(ctx.operationalContext());
    }

    public CashierOperationalContextView select(
        TchRequestContext ctx,
        SelectCashierOperationalContextRequest request
    ) {
        var tenantId = ctx.effectiveTenantIdRequired();
        var actorUserId = ctx.userId();

        queryBus.ask(new ValidateTerminalForOperationQuery(
            tenantId,
            request.terminalId(),
            request.outletId(),
            actorUserId,
            TerminalOperation.SELL_TICKET
        ));
        queryBus.ask(new ValidateOutletForOperationQuery(
            tenantId,
            request.outletId(),
            OutletOperation.SELL
        ));
        queryBus.ask(new ValidateSalesSessionForOperationQuery(
            tenantId,
            request.salesSessionId(),
            request.terminalId(),
            request.outletId(),
            actorUserId,
            SalesSessionOperation.SELL
        ));

        // Selection is validated. Trust/source of any subsequent request is decided by the
        // upstream context filter, not by this endpoint.
        return new CashierOperationalContextView(
            request.terminalId(),
            request.outletId(),
            request.salesSessionId(),
            null,
            null
        );
    }

    public void clear(TchRequestContext ctx) {
        // No-op: selection lives in client-supplied headers. Logging / audit hook can be added
        // when server-side persistence is introduced.
    }

    /**
     * Returns the outlets and terminals available to the current user.
     *
     * <p>For CASHIER / OPERATOR: returns only terminals explicitly assigned to this user,
     * plus the outlets those terminals belong to.
     *
     * <p>For TENANT_ADMIN / SUPER_ADMIN: returns all outlets and all their terminals
     * (admin may need to operate from any workstation).
     *
     * <p>Mobile auto-select rule: when {@code outlets.size() == 1 && terminals.size() == 1},
     * the client SHOULD skip the picker and directly call {@code POST /select} using
     * {@code defaults.outletId} and {@code defaults.terminalId}.
     */
    public CashierOpContextOptionsView options(TchRequestContext ctx) {
        var tenantId = ctx.effectiveTenantIdRequired();

        List<OutletOption> outletOptions;
        List<TerminalOption> terminalOptions;

        if (ctx.isOperationalRole()) {
            // Assigned role: show only this user's terminals and their parent outlets
            var userId = ctx.currentUserIdRequired();
            var criteria = new TerminalSearchCriteria(null, null, userId, null, null, null, null);
            var terminals = queryBus.ask(new ListTerminalsQuery(
                    criteria, new TchPageRequest(PageRequest.of(0, 50))))
                .items();

            var outletIds = terminals.stream()
                .map(TerminalSummaryView::outletId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

            var outlets = outletIds.stream()
                .map(id -> queryBus.ask(new GetOutletByIdQuery(tenantId, id)))
                .filter(Objects::nonNull)
                .toList();

            outletOptions = outlets.stream()
                .map(o -> new OutletOption(
                    o.id(),
                    o.name(),
                    o.kind() != null ? o.kind().name() : null))
                .toList();

            terminalOptions = terminals.stream()
                .map(t -> new TerminalOption(
                    t.id(),
                    t.outletId(),
                    t.label(),
                    t.kind() != null ? t.kind().name() : null,
                    !t.salesBlocked() && !t.locked()))
                .toList();

        } else {
            // Admin role: all outlets + all their terminals
            var outlets = queryBus.ask(new ListOutletsByTenantQuery(tenantId));

            outletOptions = outlets.stream()
                .map(o -> new OutletOption(
                    o.id(),
                    o.name(),
                    o.kind() != null ? o.kind().name() : null))
                .toList();

            terminalOptions = outlets.stream()
                .flatMap(o -> {
                    List<OutletTerminalView> ts = queryBus.ask(new ListOutletTerminalsQuery(o.id()));
                    return ts.stream().map(t -> new TerminalOption(
                        t.terminalId(),
                        t.outletId(),
                        t.label(),
                        t.kind(),
                        t.activeForUser()));
                })
                .toList();
        }

        // Auto-select defaults: set when there is exactly one choice
        var defaultOutletId = outletOptions.size() == 1 ? outletOptions.get(0).outletId() : null;
        var defaultTerminalId = terminalOptions.size() == 1 ? terminalOptions.get(0).terminalId() : null;

        return new CashierOpContextOptionsView(
            outletOptions,
            terminalOptions,
            new DefaultSelection(defaultOutletId, defaultTerminalId));
    }
}
