package com.tchalanet.server.core.payout.internal.infra.web;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.types.id.PayoutId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.payout.api.command.ExecutePayoutCommand;
import com.tchalanet.server.core.payout.internal.infra.web.mapper.PayoutWebMapper;
import com.tchalanet.server.core.payout.internal.infra.web.model.ExecutePayoutRequest;
import com.tchalanet.server.core.payout.internal.infra.web.model.PayoutWorkflowResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    private final PayoutWebMapper mapper;

    @PostMapping("/{payoutId}/execute")
    public ApiResponse<PayoutWorkflowResponse> execute(
        @CurrentContext TchRequestContext ctx,
        @PathVariable PayoutId payoutId,
        @Valid @RequestBody ExecutePayoutRequest body) {

        validateTrustedPayoutContext(ctx, body);

        var result = commandBus.execute(new ExecutePayoutCommand(
            ctx.effectiveTenantIdRequired(),
            payoutId,
            ctx.userId(),
            body.payingSessionId(),
            body.payingOutletId(),
            body.terminalId(),
            body.reason()));

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
