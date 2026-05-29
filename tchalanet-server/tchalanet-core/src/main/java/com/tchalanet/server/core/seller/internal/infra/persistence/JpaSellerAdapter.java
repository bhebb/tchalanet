package com.tchalanet.server.core.seller.internal.infra.persistence;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SellerId;
import com.tchalanet.server.common.types.id.SellerCommissionPolicyId;
import com.tchalanet.server.common.types.id.SellerOutletAssignmentId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.seller.api.model.SellerAssignmentStatus;
import com.tchalanet.server.core.seller.api.model.SellerCommissionBase;
import com.tchalanet.server.core.seller.api.model.SellerCommissionType;
import com.tchalanet.server.core.seller.api.model.SellerStatus;
import com.tchalanet.server.common.web.error.ProblemRestException;
import com.tchalanet.server.core.seller.internal.application.port.out.SellerReaderPort;
import com.tchalanet.server.core.seller.internal.application.port.out.SellerWriterPort;
import com.tchalanet.server.core.seller.internal.domain.model.Seller;
import com.tchalanet.server.core.seller.internal.domain.model.SellerCommissionPolicy;
import com.tchalanet.server.core.seller.internal.domain.model.SellerOutletAssignment;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class JpaSellerAdapter implements SellerReaderPort, SellerWriterPort {

    private final SellerRepository sellerRepo;
    private final SellerOutletAssignmentRepository assignmentRepo;
    private final SellerCommissionPolicyRepository policyRepo;

    // ---- SellerReaderPort ----

    @Override
    public Optional<Seller> findSeller(TenantId tenantId, SellerId sellerId) {
        return sellerRepo.findById(sellerId.value()).map(this::toDomain);
    }

    @Override
    public Seller getSellerRequired(TenantId tenantId, SellerId sellerId) {
        return findSeller(tenantId, sellerId)
            .orElseThrow(() -> new IllegalArgumentException("seller.not_found"));
    }

    @Override
    public Optional<Seller> findSellerByUserId(TenantId tenantId, UserId userId) {
        return sellerRepo.findByUserId(userId.value()).map(this::toDomain);
    }

    @Override
    public List<Seller> listSellers(TenantId tenantId) {
        return sellerRepo.findAllByOrderByCreatedAtDesc().stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<SellerOutletAssignment> findActiveAssignment(TenantId tenantId, SellerId sellerId) {
        return assignmentRepo.findActiveAssignment(sellerId.value()).map(this::toAssignmentDomain);
    }

    @Override
    public Optional<SellerOutletAssignment> findAssignment(TenantId tenantId, SellerOutletAssignmentId assignmentId) {
        return assignmentRepo.findById(assignmentId.value()).map(this::toAssignmentDomain);
    }

    @Override
    public List<SellerOutletAssignment> listAssignments(TenantId tenantId, SellerId sellerId) {
        return assignmentRepo.findBySellerId(sellerId.value()).stream().map(this::toAssignmentDomain).toList();
    }

    @Override
    public Optional<SellerOutletAssignment> findActiveAssignmentForOutlet(TenantId tenantId, UserId userId, OutletId outletId) {
        return assignmentRepo.findActiveForUserAndOutlet(userId.value(), outletId.value()).map(this::toAssignmentDomain);
    }

    @Override
    public Optional<SellerCommissionPolicy> findActiveCommissionPolicy(TenantId tenantId, SellerId sellerId) {
        return policyRepo.findActivePolicy(sellerId.value()).map(this::toPolicyDomain);
    }

    @Override
    public Optional<SellerCommissionPolicy> findCommissionPolicyAt(TenantId tenantId, SellerId sellerId, Instant at) {
        return policyRepo.findPolicyAt(sellerId.value(), at).map(this::toPolicyDomain);
    }

    @Override
    public Optional<SellerCommissionPolicy> findCommissionPolicy(TenantId tenantId, SellerCommissionPolicyId policyId) {
        return policyRepo.findById(policyId.value()).map(this::toPolicyDomain);
    }

    // ---- SellerWriterPort ----

    @Override
    public Seller saveSeller(Seller seller) {
        try {
            var entity = toEntity(seller);
            return toDomain(sellerRepo.save(entity));
        } catch (DataIntegrityViolationException ex) {
            // Translate constraint violation (e.g. uq_seller_tenant_code) to a 409
            // so callers receive a structured conflict error instead of a 500.
            throw ProblemRestException.conflict("seller.code_already_exists");
        }
    }

    @Override
    public SellerOutletAssignment saveAssignment(SellerOutletAssignment assignment) {
        var entity = toAssignmentEntity(assignment);
        return toAssignmentDomain(assignmentRepo.save(entity));
    }

    @Override
    public SellerCommissionPolicy saveCommissionPolicy(SellerCommissionPolicy policy) {
        var entity = toPolicyEntity(policy);
        return toPolicyDomain(policyRepo.save(entity));
    }

    // ---- Mapping helpers ----

    private Seller toDomain(SellerJpaEntity e) {
        return new Seller(
            SellerId.of(e.getId()),
            TenantId.of(e.getTenantId()),
            e.getUserId() == null ? null : UserId.of(e.getUserId()),
            e.getCode(),
            e.getDisplayName(),
            SellerStatus.valueOf(e.getStatus()),
            e.getCreatedAt(),
            e.getUpdatedAt()
        );
    }

    private SellerJpaEntity toEntity(Seller seller) {
        var e = sellerRepo.findById(seller.id().value()).orElseGet(SellerJpaEntity::new);
        e.setId(seller.id().value());
        e.setTenantId(seller.tenantId().value());
        e.setUserId(seller.userId() == null ? null : seller.userId().value());
        e.setCode(seller.code());
        e.setDisplayName(seller.displayName());
        e.setStatus(seller.status().name());
        return e;
    }

    private SellerOutletAssignment toAssignmentDomain(SellerOutletAssignmentJpaEntity e) {
        return new SellerOutletAssignment(
            SellerOutletAssignmentId.of(e.getId()),
            TenantId.of(e.getTenantId()),
            SellerId.of(e.getSellerId()),
            OutletId.of(e.getOutletId()),
            e.getStartsAt(),
            e.getEndsAt(),
            SellerAssignmentStatus.valueOf(e.getStatus()),
            e.getCreatedAt(),
            e.getUpdatedAt()
        );
    }

    private SellerOutletAssignmentJpaEntity toAssignmentEntity(SellerOutletAssignment a) {
        var e = assignmentRepo.findById(a.id().value()).orElseGet(SellerOutletAssignmentJpaEntity::new);
        e.setId(a.id().value());
        e.setTenantId(a.tenantId().value());
        e.setSellerId(a.sellerId().value());
        e.setOutletId(a.outletId().value());
        e.setStartsAt(a.startsAt());
        e.setEndsAt(a.endsAt());
        e.setStatus(a.status().name());
        return e;
    }

    private SellerCommissionPolicy toPolicyDomain(SellerCommissionPolicyJpaEntity e) {
        return new SellerCommissionPolicy(
            SellerCommissionPolicyId.of(e.getId()),
            TenantId.of(e.getTenantId()),
            SellerId.of(e.getSellerId()),
            SellerCommissionType.valueOf(e.getType()),
            SellerCommissionBase.valueOf(e.getBase()),
            e.getRatePercent(),
            e.getFixedAmount(),
            e.getCurrency(),
            e.getStartsAt(),
            e.getEndsAt(),
            SellerAssignmentStatus.valueOf(e.getStatus()),
            e.getCreatedAt(),
            e.getUpdatedAt()
        );
    }

    private SellerCommissionPolicyJpaEntity toPolicyEntity(SellerCommissionPolicy p) {
        var e = policyRepo.findById(p.id().value()).orElseGet(SellerCommissionPolicyJpaEntity::new);
        e.setId(p.id().value());
        e.setTenantId(p.tenantId().value());
        e.setSellerId(p.sellerId().value());
        e.setType(p.type().name());
        e.setBase(p.base().name());
        e.setRatePercent(p.ratePercent());
        e.setFixedAmount(p.fixedAmount());
        e.setCurrency(p.currency());
        e.setStartsAt(p.startsAt());
        e.setEndsAt(p.endsAt());
        e.setStatus(p.status().name());
        return e;
    }
}
