package com.tchalanet.server.core.outlet.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.outlet.api.query.OutletOperation;
import com.tchalanet.server.core.outlet.api.query.ValidateOutletForOperationQuery;
import com.tchalanet.server.core.outlet.api.query.ValidatedOutletOperationView;
import com.tchalanet.server.core.outlet.internal.application.port.out.OutletReaderPort;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ValidateOutletForOperationQueryHandler
    implements QueryHandler<ValidateOutletForOperationQuery, ValidatedOutletOperationView> {

    private final OutletReaderPort outletReader;

    @Override
    public ValidatedOutletOperationView handle(ValidateOutletForOperationQuery q) {
        var outlet = outletReader.getRequired(q.outletId());

        if (outlet.dayClosed()) {
            throw ProblemRest.forbidden("outlet.suspended");
        }

        if (q.operation() == OutletOperation.SELL && outlet.salesBlocked()) {
            throw ProblemRest.forbidden("outlet.sales_blocked");
        }

        if (q.operation() == OutletOperation.PAYOUT && outlet.payoutBlocked()) {
            throw ProblemRest.forbidden("outlet.payout_disabled");
        }

        if (q.operation() == OutletOperation.OFFLINE_GRANT && outlet.offlineSalesBlocked()) {
            throw ProblemRest.forbidden("outlet.offline_sales_disabled");
        }

        return new ValidatedOutletOperationView(
            outlet.id(),
            outlet.name(),
            !outlet.salesBlocked(),
            !outlet.payoutBlocked(),
            !outlet.offlineSalesBlocked()
        );
    }
}
