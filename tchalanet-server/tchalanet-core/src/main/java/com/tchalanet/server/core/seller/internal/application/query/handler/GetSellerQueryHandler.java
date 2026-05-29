package com.tchalanet.server.core.seller.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.seller.api.model.SellerView;
import com.tchalanet.server.core.seller.api.query.GetSellerQuery;
import com.tchalanet.server.core.seller.internal.application.port.out.SellerReaderPort;
import com.tchalanet.server.core.seller.internal.application.service.SellerApplicationMapper;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetSellerQueryHandler implements QueryHandler<GetSellerQuery, SellerView> {

    private final SellerReaderPort reader;
    private final SellerApplicationMapper mapper;

    @Override
    public SellerView handle(GetSellerQuery query) {
        var seller = reader.getSellerRequired(query.tenantId(), query.sellerId());
        return mapper.toSellerView(seller);
    }
}
