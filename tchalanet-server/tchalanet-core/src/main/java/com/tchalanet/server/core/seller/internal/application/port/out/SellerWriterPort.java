package com.tchalanet.server.core.seller.internal.application.port.out;

import com.tchalanet.server.core.seller.internal.domain.model.Seller;
import com.tchalanet.server.core.seller.internal.domain.model.SellerCommissionPolicy;
import com.tchalanet.server.core.seller.internal.domain.model.SellerOutletAssignment;

public interface SellerWriterPort {
    Seller saveSeller(Seller seller);
    SellerOutletAssignment saveAssignment(SellerOutletAssignment assignment);
    SellerCommissionPolicy saveCommissionPolicy(SellerCommissionPolicy policy);
}
