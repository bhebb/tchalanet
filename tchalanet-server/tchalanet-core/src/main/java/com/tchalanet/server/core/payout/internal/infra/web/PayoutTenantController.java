package com.tchalanet.server.core.payout.internal.infra.web;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.types.id.PayoutId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.payout.api.command.ExecutePayoutCommand;
import com.tchalanet.server.core.payout.internal.infra.web.mapper.PayoutWebMapper;
import com.tchalanet.server.core.payout.internal.infra.web.model.ExecutePayoutRequest;
import com.tchalanet.server.core.payout.internal.infra.web.model.PayoutWorkflowResponse;
import com.tchalanet.server.core.terminal.api.query.TerminalDeviceProofGate;
import com.tchalanet.server.core.terminal.api.query.TerminalProofPurpose;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tenant/payouts")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('CASHIER', 'TENANT_ADMIN', 'SUPER_ADMIN')")
@Tags({@Tag(name = "Payouts • Tenant")})
@Validated
public class PayoutTenantController {

    private final CommandBus commandBus;
    private final QueryBus queryBus;
    private final PayoutWebMapper mapper;

    @PostMapping("/{payoutId}/execute")
    public ApiResponse<PayoutWorkflowResponse> execute(
        @CurrentContext TchRequestContext ctx,
        @RequestHeader(TerminalDeviceProofGate.HEADER_TERMINAL_ID) String terminalId,
        @RequestHeader(TerminalDeviceProofGate.HEADER_BINDING_ID)  String bindingId,
        @RequestHeader(TerminalDeviceProofGate.HEADER_NONCE)       String nonce,
        @RequestHeader(TerminalDeviceProofGate.HEADER_SIGNED_AT)   String signedAt,
        @RequestHeader(TerminalDeviceProofGate.HEADER_SIGNATURE)   String signature,
        @PathVariable PayoutId payoutId,
        @Valid @RequestBody ExecutePayoutRequest body) {

        TerminalDeviceProofGate.verify(queryBus, ctx.effectiveTenantIdRequired(),
            terminalId, bindingId, TerminalProofPurpose.PAYOUT_CONFIRM,
            "POST", "/tenant/payouts/" + payoutId.value() + "/execute",
            null, ctx.operationalContext(), nonce, signedAt, signature);
        validateTrustedPayoutContext(ctx, body);

        var result = commandBus.execute(new ExecutePayoutCommand(
            ctx.effectiveTenantIdRequired(), payoutId, ctx.userId(),
            body.payingSessionId(), body.payingOutletId(), body.terminalId(), body.reason()));

        return ApiResponse.success(mapper.toResponse(result));
    }

    private void validateTrustedPayoutContext(TchRequestContext ctx, ExecutePayoutRequest body) {
        var op = ctx.trustedOperationalContextRequired();
        if (!body.terminalId().equals(op.terminalId())
            || !body.payingOutletId().equals(op.outletId())
            || !body.payingSessionId().equals(op.salesSessionId())) {
            throw ProblemRest.forbidden("operational_context.mismatch");
        }
    }
}
