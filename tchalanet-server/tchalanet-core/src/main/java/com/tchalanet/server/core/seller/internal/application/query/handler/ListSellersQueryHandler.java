package com.tchalanet.server.core.seller.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.seller.api.query.ListSellersQuery;
import com.tchalanet.server.core.seller.api.query.model.SellerSummaryView;
import com.tchalanet.server.core.seller.internal.application.port.out.SellerReaderPort;
import com.tchalanet.server.core.seller.internal.application.service.SellerApplicationMapper;
import lombok.RequiredArgsConstructor;

import java.util.List;

@UseCase
@RequiredArgsConstructor
public class ListSellersQueryHandler implements QueryHandler<ListSellersQuery, List<SellerSummaryView>> {

    private final SellerReaderPort reader;
    private final SellerApplicationMapper mapper;

    @Override
    public List<SellerSummaryView> handle(ListSellersQuery query) {
        return reader.listSellers(query.tenantId()).stream()
            .map(mapper::toSellerSummaryView)
            .toList();
    }
}
