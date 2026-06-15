package com.tchalanet.server.core.offlinesync.internal.infra.web;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.types.id.OfflineSubmissionId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.core.offlinesync.api.command.grant.RequestOfflineGrantCommand;
import com.tchalanet.server.core.offlinesync.api.command.grant.RequestOfflineGrantResult;
import com.tchalanet.server.core.offlinesync.api.command.sync.SyncOfflineSalesCommand;
import com.tchalanet.server.core.offlinesync.api.command.sync.SyncOfflineSalesResult;
import com.tchalanet.server.core.offlinesync.api.model.submission.OfflineSubmissionStatus;
import com.tchalanet.server.core.offlinesync.api.query.grant.GetCurrentOfflineGrantQuery;
import com.tchalanet.server.core.offlinesync.api.query.grant.OfflineGrantView;
import com.tchalanet.server.core.offlinesync.api.query.submission.GetOfflineSubmissionQuery;
import com.tchalanet.server.core.offlinesync.api.query.submission.ListOfflineSubmissionsQuery;
import com.tchalanet.server.core.offlinesync.api.query.submission.OfflineSubmissionView;
import com.tchalanet.server.core.offlinesync.internal.infra.web.model.OfflineGrantIssuedResponse;
import com.tchalanet.server.core.offlinesync.internal.infra.web.model.RequestOfflineGrantRequest;
import com.tchalanet.server.core.offlinesync.internal.infra.web.model.SyncOfflineSalesRequest;
import com.tchalanet.server.core.terminal.api.query.TerminalDeviceProofGate;
import com.tchalanet.server.core.terminal.api.query.TerminalProofPurpose;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

/**
 * POS / cashier-facing offline endpoints.
 *
 * <p>Grant issuance and sync ingestion require a trusted operational context. The auth role
 * {@code AGENT} suffices for the cashier flows; admin overrides live in
 * {@link OfflineAdminController}.
 */
@RestController
@RequestMapping("/tenant/offline")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ACTOR_SELLER_TERMINAL') or hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'SUPER_ADMIN')")
@Tags({@Tag(name = "Offline sync • Tenant")})
@Validated
public class OfflineTenantController {

    private final CommandBus commandBus;
    private final QueryBus queryBus;

    @PostMapping("/grants")
    public ApiResponse<OfflineGrantIssuedResponse> requestGrant(
        @CurrentContext TchRequestContext ctx,
        @RequestHeader(TerminalDeviceProofGate.HEADER_TERMINAL_ID) String terminalId,
        @RequestHeader(TerminalDeviceProofGate.HEADER_BINDING_ID)  String bindingId,
        @RequestHeader(TerminalDeviceProofGate.HEADER_NONCE)       String nonce,
        @RequestHeader(TerminalDeviceProofGate.HEADER_SIGNED_AT)   String signedAt,
        @RequestHeader(TerminalDeviceProofGate.HEADER_SIGNATURE)   String signature,
        @Valid @RequestBody RequestOfflineGrantRequest body
    ) {
        var op = ctx.operationalContext();
        TerminalDeviceProofGate.verify(queryBus, ctx.effectiveTenantIdRequired(),
            terminalId, bindingId, TerminalProofPurpose.OFFLINE_GRANT_REQUEST,
            "POST", "/tenant/offline/grants", null, op, nonce, signedAt, signature);

        if (op == null || !op.trustedForSensitiveOperation()) {
            throw new IllegalStateException(
                "offlinesync.context_untrusted: trusted operational context required");
        }

        RequestOfflineGrantResult result = commandBus.execute(new RequestOfflineGrantCommand(
            ctx.effectiveTenantIdRequired(),
            ctx.currentUserIdRequired(),
            op.terminalId(),
            op.outletId(),
            op.salesSessionId(),
            body.deviceId(),
            body.devicePublicKey(),
            body.keyId()
        ));

        return ApiResponse.success(new OfflineGrantIssuedResponse(
            result.grantId(), result.codeBatchId(),
            result.validFrom(), result.validUntil(), result.syncAcceptedUntil(),
            result.maxTicketCount(), result.currency(),
            result.offlineCodes(),
            result.grantSignature(), result.serverPublicKey(),
            result.upcomingDraws()
        ));
    }

    @GetMapping("/grants/current")
    public ApiResponse<OfflineGrantView> currentGrant(
        @CurrentContext TchRequestContext ctx,
        @RequestParam("deviceId") java.util.UUID deviceId
    ) {
        var op = ctx.operationalContext();
        if (op == null || op.terminalId() == null) {
            throw new IllegalStateException("offlinesync.context_untrusted: terminal required");
        }
        var view = queryBus.ask(new GetCurrentOfflineGrantQuery(
            ctx.effectiveTenantIdRequired(),
            ctx.currentUserIdRequired(),
            op.terminalId(),
            deviceId
        ));
        return ApiResponse.success(view);
    }

    @PostMapping("/sync")
    public ApiResponse<SyncOfflineSalesResult> sync(
        @CurrentContext TchRequestContext ctx,
        @RequestHeader(TerminalDeviceProofGate.HEADER_TERMINAL_ID) String terminalId,
        @RequestHeader(TerminalDeviceProofGate.HEADER_BINDING_ID)  String bindingId,
        @RequestHeader(TerminalDeviceProofGate.HEADER_NONCE)       String nonce,
        @RequestHeader(TerminalDeviceProofGate.HEADER_SIGNED_AT)   String signedAt,
        @RequestHeader(TerminalDeviceProofGate.HEADER_SIGNATURE)   String signature,
        @Valid @RequestBody SyncOfflineSalesRequest body
    ) {
        var op = ctx.operationalContext();
        TerminalDeviceProofGate.verify(queryBus, ctx.effectiveTenantIdRequired(),
            terminalId, bindingId, TerminalProofPurpose.OFFLINE_SYNC,
            "POST", "/tenant/offline/sync", null, op, nonce, signedAt, signature);

        if (op == null || !op.trustedForSensitiveOperation()) {
            throw new IllegalStateException(
                "offlinesync.context_untrusted: trusted operational context required");
        }
        var submissions = body.submissions().stream()
            .map(s -> new SyncOfflineSalesCommand.Submission(
                s.clientSubmissionId(), s.offlineCode(), s.drawId(), s.clientSoldAt(),
                s.totalStakeAmount(),
                s.lines().stream().map(l -> new SyncOfflineSalesCommand.Line(
                    l.lineNo(), l.gameCode(), l.betType(), l.betOption(),
                    l.selectionKey(), l.stakeAmount(), l.potentialPayout()
                )).toList(),
                s.payloadHash(), s.signature()
            ))
            .toList();
        var result = commandBus.execute(new SyncOfflineSalesCommand(
            ctx.effectiveTenantIdRequired(), body.grantId(),
            body.clientBatchId(), body.batchPayloadHash(),
            submissions, op.trustedForSensitiveOperation()
        ));
        return ApiResponse.success(result);
    }

    @GetMapping("/submissions/my")
    public ApiResponse<List<OfflineSubmissionView>> mySubmissions(
        @CurrentContext TchRequestContext ctx,
        @RequestParam(value = "status", required = false) Set<OfflineSubmissionStatus> statuses,
        @RequestParam(value = "limit", defaultValue = "50") int limit
    ) {
        var views = queryBus.ask(new ListOfflineSubmissionsQuery(
            ctx.effectiveTenantIdRequired(),
            ctx.currentUserIdRequired(),
            statuses == null ? Set.of() : statuses,
            limit
        ));
        return ApiResponse.success(views);
    }

    @GetMapping("/submissions/{submissionId}/status")
    public ApiResponse<OfflineSubmissionView> submissionStatus(
        @CurrentContext TchRequestContext ctx,
        @PathVariable OfflineSubmissionId submissionId
    ) {
        var view = queryBus.ask(new GetOfflineSubmissionQuery(
            ctx.effectiveTenantIdRequired(),
            submissionId
        ));
        return ApiResponse.success(view);
    }
}
