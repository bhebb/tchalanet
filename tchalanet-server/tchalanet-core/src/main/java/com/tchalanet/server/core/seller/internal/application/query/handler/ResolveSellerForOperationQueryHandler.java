package com.tchalanet.server.core.seller.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.seller.api.model.SellerOperationalView;
import com.tchalanet.server.core.seller.api.query.ResolveSellerForOperationQuery;
import com.tchalanet.server.core.seller.internal.application.port.out.SellerReaderPort;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ResolveSellerForOperationQueryHandler implements QueryHandler<ResolveSellerForOperationQuery, SellerOperationalView> {

    private final SellerReaderPort reader;

    @Override
    public SellerOperationalView handle(ResolveSellerForOperationQuery query) {
        var seller = reader.findSellerByUserId(query.tenantId(), query.userId())
            .orElseThrow(() -> ProblemRest.forbidden("seller.no_seller_for_user"));

        if (!seller.activeForSale()) {
            throw ProblemRest.forbidden("seller.not_active");
        }

        var assignment = reader.findActiveAssignmentForOutlet(
                query.tenantId(), query.userId(), query.outletId())
            .orElseThrow(() -> ProblemRest.forbidden("seller.not_assigned_to_outlet"));

        return new SellerOperationalView(
            seller.id(),
            assignment.id(),
            assignment.outletId(),
            seller.status(),
            assignment.status(),
            true
        );
    }
}
