package com.tchalanet.server.features.cashier.operationalcontext.app;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.core.outlet.api.query.OutletOperation;
import com.tchalanet.server.core.outlet.api.query.ValidateOutletForOperationQuery;
import com.tchalanet.server.core.session.api.query.SalesSessionOperation;
import com.tchalanet.server.core.session.api.query.ValidateSalesSessionForOperationQuery;
import com.tchalanet.server.core.terminal.api.query.TerminalOperation;
import com.tchalanet.server.core.terminal.api.query.ValidateTerminalForOperationQuery;
import com.tchalanet.server.features.cashier.operationalcontext.model.CashierOperationalContextView;
import com.tchalanet.server.features.cashier.operationalcontext.model.SelectCashierOperationalContextRequest;
import lombok.RequiredArgsConstructor;
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
            TerminalOperation.SELL
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
}
