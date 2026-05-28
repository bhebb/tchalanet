package com.tchalanet.server.core.payout.internal.infra.web;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.types.id.PayoutId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.core.payout.api.command.BlockPayoutClaimCommand;
import com.tchalanet.server.core.payout.api.command.CancelPayoutClaimCommand;
import com.tchalanet.server.core.payout.api.command.ReversePayoutPaymentCommand;
import com.tchalanet.server.core.payout.api.command.UnblockPayoutClaimCommand;
import com.tchalanet.server.core.payout.internal.infra.web.mapper.PayoutWebMapper;
import com.tchalanet.server.core.payout.internal.infra.web.model.PayoutWorkflowResponse;
import com.tchalanet.server.core.payout.internal.infra.web.model.RejectPayoutRequest;
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
@RequestMapping("/admin/payouts")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'SUPER_ADMIN')")
@Tags({@Tag(name = "Payouts • Admin")})
@Validated
public class PayoutAdminController {

    private final CommandBus commandBus;
    private final PayoutWebMapper mapper;

    @PostMapping("/{payoutId}/block")
    public ApiResponse<PayoutWorkflowResponse> block(
        @CurrentContext TchRequestContext ctx,
        @PathVariable PayoutId payoutId,
        @Valid @RequestBody RejectPayoutRequest body) {

        var result = commandBus.execute(new BlockPayoutClaimCommand(
            ctx.effectiveTenantIdRequired(),
            payoutId,
            ctx.userId(),
            body.reason()));

        return ApiResponse.success(mapper.toResponse(result));
    }

    @PostMapping("/{payoutId}/unblock")
    public ApiResponse<PayoutWorkflowResponse> unblock(
        @CurrentContext TchRequestContext ctx,
        @PathVariable PayoutId payoutId) {

        var result = commandBus.execute(new UnblockPayoutClaimCommand(
            ctx.effectiveTenantIdRequired(),
            payoutId,
            ctx.userId()));

        return ApiResponse.success(mapper.toResponse(result));
    }

    @PostMapping("/{payoutId}/cancel")
    public ApiResponse<PayoutWorkflowResponse> cancel(
        @CurrentContext TchRequestContext ctx,
        @PathVariable PayoutId payoutId,
        @Valid @RequestBody RejectPayoutRequest body) {

        var result = commandBus.execute(new CancelPayoutClaimCommand(
            ctx.effectiveTenantIdRequired(),
            payoutId,
            ctx.userId(),
            body.reason()));

        return ApiResponse.success(mapper.toResponse(result));
    }

    @PostMapping("/{payoutId}/reverse")
    public ApiResponse<PayoutWorkflowResponse> reverse(
        @CurrentContext TchRequestContext ctx,
        @PathVariable PayoutId payoutId,
        @Valid @RequestBody RejectPayoutRequest body) {

        var result = commandBus.execute(new ReversePayoutPaymentCommand(
            ctx.effectiveTenantIdRequired(),
            payoutId,
            ctx.userId(),
            body.reason()));

        return ApiResponse.success(mapper.toResponse(result));
    }
}
