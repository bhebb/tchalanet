package com.tchalanet.server.core.seller.internal.application.service;

import com.tchalanet.server.core.seller.api.model.SellerView;
import com.tchalanet.server.core.seller.api.query.model.SellerSummaryView;
import com.tchalanet.server.core.seller.internal.domain.model.Seller;
import org.springframework.stereotype.Component;

@Component
public class SellerApplicationMapper {

    public SellerView toSellerView(Seller seller) {
        return new SellerView(
            seller.id(),
            seller.userId(),
            seller.code(),
            seller.displayName(),
            seller.status(),
            seller.createdAt(),
            seller.updatedAt()
        );
    }

    public SellerSummaryView toSellerSummaryView(Seller seller) {
        return new SellerSummaryView(
            seller.id(),
            seller.userId(),
            seller.code(),
            seller.displayName(),
            seller.status().name(),
            seller.createdAt()
        );
    }
}
