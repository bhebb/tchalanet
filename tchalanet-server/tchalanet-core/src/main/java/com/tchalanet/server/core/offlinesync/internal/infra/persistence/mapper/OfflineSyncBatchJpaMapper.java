package com.tchalanet.server.core.offlinesync.internal.infra.persistence.mapper;

import com.tchalanet.server.common.types.id.OfflineCodeBatchId;
import com.tchalanet.server.common.types.id.OfflineGrantId;
import com.tchalanet.server.common.types.id.OfflineSyncBatchId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.offlinesync.api.model.syncbatch.OfflineSyncBatchStatus;
import com.tchalanet.server.core.offlinesync.api.model.syncbatch.SyncBatchCounters;
import com.tchalanet.server.core.offlinesync.api.model.syncbatch.SyncBatchLifecycle;
import com.tchalanet.server.core.offlinesync.internal.domain.model.syncbatch.OfflineSyncBatch;
import com.tchalanet.server.core.offlinesync.internal.domain.model.syncbatch.SyncBatchContext;
import com.tchalanet.server.core.offlinesync.internal.domain.model.syncbatch.SyncBatchIdentity;
import com.tchalanet.server.core.offlinesync.internal.infra.persistence.OfflineSyncBatchJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class OfflineSyncBatchJpaMapper {

    public OfflineSyncBatch toDomain(OfflineSyncBatchJpaEntity e) {
        return new OfflineSyncBatch(
            new SyncBatchIdentity(
                OfflineSyncBatchId.of(e.getId()),
                TenantId.of(e.getTenantId()),
                OfflineGrantId.of(e.getGrantId()),
                e.getCodeBatchId() != null ? OfflineCodeBatchId.of(e.getCodeBatchId()) : null,
                e.getClientBatchId()
            ),
            new SyncBatchContext(
                e.getDeviceId(),
                UserId.of(e.getSellerUserId()),
                TerminalId.of(e.getTerminalId()),
                OutletId.of(e.getOutletId()),
                SalesSessionId.of(e.getSalesSessionId())
            ),
            new SyncBatchCounters(
                e.getSubmissionCount(),
                e.getTechnicalRejectCount(),
                e.getSalesAcceptCount(),
                e.getSalesRejectCount(),
                e.getReviewCount()
            ),
            new SyncBatchLifecycle(
                OfflineSyncBatchStatus.valueOf(e.getStatus()),
                e.getReceivedAt(),
                e.getProcessedAt()
            )
        );
    }

    public OfflineSyncBatchJpaEntity toEntity(OfflineSyncBatch b, OfflineSyncBatchJpaEntity target) {
        OfflineSyncBatchJpaEntity e = target != null ? target : new OfflineSyncBatchJpaEntity();
        e.setId(b.identity().id().value());
        e.setTenantId(b.identity().tenantId().value());
        e.setGrantId(b.identity().grantId().value());
        e.setCodeBatchId(b.identity().codeBatchId() != null ? b.identity().codeBatchId().value() : null);
        e.setClientBatchId(b.identity().clientBatchId());
        e.setDeviceId(b.context().deviceId());
        e.setSellerUserId(b.context().sellerUserId().value());
        e.setTerminalId(b.context().terminalId().value());
        e.setOutletId(b.context().outletId().value());
        e.setSalesSessionId(b.context().salesSessionId().value());
        e.setReceivedAt(b.lifecycle().receivedAt());
        e.setProcessedAt(b.lifecycle().processedAt());
        e.setStatus(b.lifecycle().status().name());
        e.setSubmissionCount(b.counters().submissionCount());
        e.setTechnicalRejectCount(b.counters().technicalRejectCount());
        e.setSalesAcceptCount(b.counters().salesAcceptCount());
        e.setSalesRejectCount(b.counters().salesRejectCount());
        e.setReviewCount(b.counters().reviewCount());
        return e;
    }
}
