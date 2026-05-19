package com.tchalanet.server.core.offlinesync.internal.infra.persistence.mapper;

import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.OfflineCodeBatchId;
import com.tchalanet.server.common.types.id.OfflineGrantId;
import com.tchalanet.server.common.types.id.OfflineSubmissionId;
import com.tchalanet.server.common.types.id.OfflineSyncBatchId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.PromotionAttemptId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.offlinesync.api.model.submission.OfflineSubmissionStatus;
import com.tchalanet.server.core.offlinesync.api.model.submission.SubmissionLifecycle;
import com.tchalanet.server.core.offlinesync.api.model.submission.SubmissionPromotionTrace;
import com.tchalanet.server.core.offlinesync.internal.domain.model.submission.OfflineSubmission;
import com.tchalanet.server.core.offlinesync.internal.domain.model.submission.SubmissionContext;
import com.tchalanet.server.core.offlinesync.internal.domain.model.submission.SubmissionIdentity;
import com.tchalanet.server.core.offlinesync.internal.domain.model.submission.SubmissionPayload;
import com.tchalanet.server.core.offlinesync.internal.infra.persistence.OfflineSubmissionJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class OfflineSubmissionJpaMapper {

    public OfflineSubmission toDomain(OfflineSubmissionJpaEntity e) {
        CurrencyCode currency = CurrencyCode.of(e.getCurrency());
        return new OfflineSubmission(
            new SubmissionIdentity(
                OfflineSubmissionId.of(e.getId()),
                TenantId.of(e.getTenantId()),
                e.getSyncBatchId() != null ? OfflineSyncBatchId.of(e.getSyncBatchId()) : null,
                OfflineGrantId.of(e.getGrantId()),
                e.getCodeBatchId() != null ? OfflineCodeBatchId.of(e.getCodeBatchId()) : null,
                e.getOfflineCode(),
                e.getClientSubmissionId()
            ),
            new SubmissionContext(
                e.getDeviceId(),
                UserId.of(e.getSellerUserId()),
                TerminalId.of(e.getTerminalId()),
                OutletId.of(e.getOutletId()),
                SalesSessionId.of(e.getSalesSessionId())
            ),
            new SubmissionPayload(
                com.tchalanet.server.common.types.id.DrawId.of(e.getDrawId()),
                e.getClientSoldAt(),
                new Money(e.getTotalStakeAmount(), currency),
                e.getLineCount(),
                e.getPayloadHash(),
                e.getSignature()
            ),
            new SubmissionLifecycle(
                OfflineSubmissionStatus.valueOf(e.getStatus()),
                e.getReceivedAt(),
                e.getProcessedAt(),
                e.getRejectionCode(),
                e.getRejectionReason()
            ),
            new SubmissionPromotionTrace(
                e.getPromotionAttemptId() != null ? PromotionAttemptId.of(e.getPromotionAttemptId()) : null,
                e.getPromotionRequestedAt(),
                e.getLastPromotionEventId() != null ? EventId.of(e.getLastPromotionEventId()) : null,
                e.getCreatedTicketId() != null ? TicketId.of(e.getCreatedTicketId()) : null
            )
        );
    }

    public OfflineSubmissionJpaEntity toEntity(OfflineSubmission s, OfflineSubmissionJpaEntity target) {
        OfflineSubmissionJpaEntity e = target != null ? target : new OfflineSubmissionJpaEntity();
        e.setId(s.identity().id().value());
        e.setTenantId(s.identity().tenantId().value());
        e.setSyncBatchId(s.identity().syncBatchId() != null ? s.identity().syncBatchId().value() : null);
        e.setGrantId(s.identity().grantId().value());
        e.setCodeBatchId(s.identity().codeBatchId() != null ? s.identity().codeBatchId().value() : null);
        e.setOfflineCode(s.identity().offlineCode());
        e.setClientSubmissionId(s.identity().clientSubmissionId());
        e.setDeviceId(s.context().deviceId());
        e.setSellerUserId(s.context().sellerUserId().value());
        e.setTerminalId(s.context().terminalId().value());
        e.setOutletId(s.context().outletId().value());
        e.setSalesSessionId(s.context().salesSessionId().value());
        e.setDrawId(s.payload().drawId().value());
        e.setClientSoldAt(s.payload().clientSoldAt());
        e.setReceivedAt(s.lifecycle().receivedAt());
        e.setProcessedAt(s.lifecycle().processedAt());
        e.setStatus(s.lifecycle().status().name());
        e.setRejectionCode(s.lifecycle().rejectionCode());
        e.setRejectionReason(s.lifecycle().rejectionReason());
        e.setTotalStakeAmount(s.payload().totalStakeAmount().amount());
        e.setCurrency(s.payload().totalStakeAmount().currency().value());
        e.setLineCount(s.payload().lineCount());
        e.setPayloadHash(s.payload().payloadHash());
        e.setSignature(s.payload().signature());
        e.setPromotionAttemptId(s.promotion().promotionAttemptId() != null
            ? s.promotion().promotionAttemptId().value() : null);
        e.setPromotionRequestedAt(s.promotion().promotionRequestedAt());
        e.setLastPromotionEventId(s.promotion().lastPromotionEventId() != null
            ? s.promotion().lastPromotionEventId().value() : null);
        e.setCreatedTicketId(s.promotion().createdTicketId() != null
            ? s.promotion().createdTicketId().value() : null);
        if (e.getRawPayload() == null) e.setRawPayload("{}");
        return e;
    }
}
