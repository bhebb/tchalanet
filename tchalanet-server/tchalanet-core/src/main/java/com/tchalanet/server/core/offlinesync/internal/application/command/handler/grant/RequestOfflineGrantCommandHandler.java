package com.tchalanet.server.core.offlinesync.internal.application.command.handler.grant;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.OfflineCodeBatchId;
import com.tchalanet.server.common.types.id.OfflineCodeId;
import com.tchalanet.server.common.types.id.OfflineGrantId;
import com.tchalanet.server.core.draw.api.query.ListUpcomingDrawsTenantWideQuery;
import com.tchalanet.server.core.draw.internal.application.query.projection.DrawSummary;
import com.tchalanet.server.core.limitpolicy.api.model.offline.OfflineLimitPolicy;
import com.tchalanet.server.core.limitpolicy.api.query.GetOfflineLimitPolicyQuery;
import com.tchalanet.server.core.offlinesync.api.command.grant.OfflineUpcomingDrawSnapshot;
import com.tchalanet.server.core.offlinesync.api.command.grant.RequestOfflineGrantCommand;
import com.tchalanet.server.core.offlinesync.api.command.grant.RequestOfflineGrantResult;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineCodeBatchWriterPort;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineCodeWriterPort;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineCryptoPort;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineGrantWriterPort;
import com.tchalanet.server.core.offlinesync.internal.application.service.grant.OfflineCodeGenerator;
import com.tchalanet.server.core.offlinesync.internal.application.service.grant.OfflineGrantPayloadSigner;
import com.tchalanet.server.core.offlinesync.internal.domain.model.code.OfflineCode;
import com.tchalanet.server.core.offlinesync.internal.domain.model.codebatch.OfflineCodeBatch;
import com.tchalanet.server.core.offlinesync.internal.domain.model.grant.OfflineGrant;
import com.tchalanet.server.core.offlinesync.internal.domain.service.OfflineGrantPolicy;
import com.tchalanet.server.core.offlinesync.internal.infra.config.OfflineUpcomingDrawsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Issues a fresh offline grant + matching code batch + N usable codes + the list of draws
 * the cashier is allowed to sell offline against. All in one transaction. The grant payload
 * is signed server-side using a canonical, versioned representation so POS devices can
 * validate it offline.
 */
@UseCase
@RequiredArgsConstructor
@Slf4j
public class RequestOfflineGrantCommandHandler
    implements CommandHandler<RequestOfflineGrantCommand, RequestOfflineGrantResult> {

    private final OfflineGrantWriterPort grantWriter;
    private final OfflineCodeBatchWriterPort codeBatchWriter;
    private final OfflineCodeWriterPort codeWriter;
    private final OfflineCryptoPort crypto;
    private final OfflineGrantPayloadSigner grantSigner;
    private final OfflineCodeGenerator codeGenerator;
    private final IdGenerator idGenerator;
    private final QueryBus queryBus;
    private final OfflineUpcomingDrawsProperties upcomingDrawsProperties;
    private final Clock clock;

    @Override
    @TchTx
    public RequestOfflineGrantResult handle(RequestOfflineGrantCommand command) {
        Instant now = clock.instant();

        OfflineLimitPolicy policy = queryBus.ask(new GetOfflineLimitPolicyQuery(command.tenantId()));

        var decision = OfflineGrantPolicy.evaluateIssue(
            new OfflineGrantPolicy.Inputs(
                policy.offlineEnabled(), policy.batchSize(),
                policy.validityDuration(), policy.syncAcceptedExtension(),
                policy.maxTicketCount(), policy.maxTotalAmount()
            ),
            now
        );
        if (decision instanceof OfflineGrantPolicy.Decision.Reject reject) {
            throw new IllegalStateException(reject.code() + ": " + reject.reason());
        }
        var accept = (OfflineGrantPolicy.Decision.Accept) decision;

        OfflineGrantId grantId = OfflineGrantId.of(idGenerator.newUuid());
        OfflineGrant grant = OfflineGrant.issue(
            grantId, command.tenantId(), command.sellerUserId(),
            command.terminalId(), command.outletId(), command.salesSessionId(),
            command.deviceId(), command.devicePublicKey(), command.keyId(),
            accept.validFrom(), accept.validUntil(), accept.syncAcceptedUntil(),
            accept.maxTicketCount(), accept.maxTotalAmount(), now
        );
        grantWriter.save(grant);

        OfflineCodeBatchId batchId = OfflineCodeBatchId.of(idGenerator.newUuid());
        OfflineCodeBatch batch = OfflineCodeBatch.open(
            batchId, command.tenantId(), grantId,
            command.terminalId(), command.outletId(), command.sellerUserId(),
            policy.batchSize(), now, accept.validUntil()
        );
        codeBatchWriter.save(batch);

        List<String> codes = new ArrayList<>(policy.batchSize());
        for (int i = 0; i < policy.batchSize(); i++) {
            String code = codeGenerator.next();
            OfflineCodeId codeId = OfflineCodeId.of(idGenerator.newUuid());
            codeWriter.save(OfflineCode.issue(
                codeId, command.tenantId(), batchId, grantId, code, accept.validUntil()));
            codes.add(code);
        }

        String grantSignature = grantSigner.sign(
            grantId, command.tenantId().value(), command.deviceId(), command.keyId(),
            accept.validFrom(), accept.validUntil(), accept.syncAcceptedUntil(),
            accept.maxTicketCount(), accept.maxTotalAmount()
        );

        List<OfflineUpcomingDrawSnapshot> upcomingDraws = loadUpcomingDraws();

        log.info("offlinesync: issued grant {} for seller {} (codes={}, upcoming-draws={})",
            grantId, command.sellerUserId(), codes.size(), upcomingDraws.size());

        return new RequestOfflineGrantResult(
            grantId, batchId,
            accept.validFrom(), accept.validUntil(), accept.syncAcceptedUntil(),
            accept.maxTicketCount(), accept.maxTotalAmount().currency().value(),
            List.copyOf(codes),
            grantSignature,
            crypto.serverPublicKey(),
            upcomingDraws
        );
    }

    private List<OfflineUpcomingDrawSnapshot> loadUpcomingDraws() {
        int lookaheadHours = upcomingDrawsProperties.lookaheadDays() * 24;
        List<DrawSummary> summaries = queryBus.ask(new ListUpcomingDrawsTenantWideQuery(
            lookaheadHours, upcomingDrawsProperties.limit()));
        return summaries.stream()
            .map(s -> new OfflineUpcomingDrawSnapshot(
                s.drawId(), s.drawChannelId(),
                s.drawChannelCode(), s.drawChannelLabel(),
                s.scheduledAt(), s.cutoffAt(),
                s.status() != null ? s.status().name() : null
            ))
            .toList();
    }
}
