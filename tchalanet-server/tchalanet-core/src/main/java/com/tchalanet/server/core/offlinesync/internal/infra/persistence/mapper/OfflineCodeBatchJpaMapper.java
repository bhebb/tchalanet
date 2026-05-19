package com.tchalanet.server.core.offlinesync.internal.infra.persistence.mapper;

import com.tchalanet.server.common.types.id.OfflineCodeBatchId;
import com.tchalanet.server.common.types.id.OfflineGrantId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.offlinesync.api.model.codebatch.CodeBatchCounts;
import com.tchalanet.server.core.offlinesync.api.model.codebatch.CodeBatchLifecycle;
import com.tchalanet.server.core.offlinesync.api.model.codebatch.OfflineCodeBatchStatus;
import com.tchalanet.server.core.offlinesync.internal.domain.model.codebatch.CodeBatchIdentity;
import com.tchalanet.server.core.offlinesync.internal.domain.model.codebatch.OfflineCodeBatch;
import com.tchalanet.server.core.offlinesync.internal.infra.persistence.OfflineCodeBatchJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class OfflineCodeBatchJpaMapper {

    public OfflineCodeBatch toDomain(OfflineCodeBatchJpaEntity e) {
        return new OfflineCodeBatch(
            new CodeBatchIdentity(
                OfflineCodeBatchId.of(e.getId()),
                TenantId.of(e.getTenantId()),
                OfflineGrantId.of(e.getGrantId()),
                TerminalId.of(e.getTerminalId()),
                e.getOutletId() != null ? OutletId.of(e.getOutletId()) : null,
                e.getSellerUserId() != null ? UserId.of(e.getSellerUserId()) : null
            ),
            new CodeBatchCounts(e.getAllocatedCount(), e.getConsumedCount()),
            new CodeBatchLifecycle(
                OfflineCodeBatchStatus.valueOf(e.getStatus()),
                e.getIssuedAt(),
                e.getExpiresAt()
            )
        );
    }

    public OfflineCodeBatchJpaEntity toEntity(OfflineCodeBatch b, OfflineCodeBatchJpaEntity target) {
        OfflineCodeBatchJpaEntity e = target != null ? target : new OfflineCodeBatchJpaEntity();
        e.setId(b.identity().id().value());
        e.setTenantId(b.identity().tenantId().value());
        e.setGrantId(b.identity().grantId().value());
        e.setTerminalId(b.identity().terminalId().value());
        e.setOutletId(b.identity().outletId() != null ? b.identity().outletId().value() : null);
        e.setSellerUserId(b.identity().sellerUserId() != null ? b.identity().sellerUserId().value() : null);
        e.setAllocatedCount(b.counts().allocatedCount());
        e.setConsumedCount(b.counts().consumedCount());
        e.setStatus(b.lifecycle().status().name());
        e.setIssuedAt(b.lifecycle().issuedAt());
        e.setExpiresAt(b.lifecycle().expiresAt());
        return e;
    }
}
