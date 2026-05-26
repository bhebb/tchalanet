package com.tchalanet.server.core.seller.internal.application.port.out;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SellerId;
import com.tchalanet.server.common.types.id.SellerCommissionPolicyId;
import com.tchalanet.server.common.types.id.SellerOutletAssignmentId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.seller.internal.domain.model.Seller;
import com.tchalanet.server.core.seller.internal.domain.model.SellerCommissionPolicy;
import com.tchalanet.server.core.seller.internal.domain.model.SellerOutletAssignment;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SellerReaderPort {
    Optional<Seller> findSeller(TenantId tenantId, SellerId sellerId);
    Seller getSellerRequired(TenantId tenantId, SellerId sellerId);
    Optional<Seller> findSellerByUserId(TenantId tenantId, UserId userId);
    List<Seller> listSellers(TenantId tenantId);
    Optional<SellerOutletAssignment> findActiveAssignment(TenantId tenantId, SellerId sellerId);
    Optional<SellerOutletAssignment> findAssignment(TenantId tenantId, SellerOutletAssignmentId assignmentId);
    List<SellerOutletAssignment> listAssignments(TenantId tenantId, SellerId sellerId);
    Optional<SellerOutletAssignment> findActiveAssignmentForOutlet(TenantId tenantId, UserId userId, OutletId outletId);
    Optional<SellerCommissionPolicy> findActiveCommissionPolicy(TenantId tenantId, SellerId sellerId);
    Optional<SellerCommissionPolicy> findCommissionPolicyAt(TenantId tenantId, SellerId sellerId, Instant at);
    Optional<SellerCommissionPolicy> findCommissionPolicy(TenantId tenantId, SellerCommissionPolicyId policyId);
}
