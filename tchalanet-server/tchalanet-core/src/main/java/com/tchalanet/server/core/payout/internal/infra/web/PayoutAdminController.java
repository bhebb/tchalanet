package com.tchalanet.server.core.payout.internal.infra.web;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.types.id.PayoutId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.core.payout.api.command.ApprovePayoutCommand;
import com.tchalanet.server.core.payout.api.command.RejectPayoutCommand;
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

    @PostMapping("/{payoutId}/approve")
    @PreAuthorize("hasPermission(null, 'PAYOUT_APPROVE')")
    public ApiResponse<PayoutWorkflowResponse> approve(
        @CurrentContext TchRequestContext ctx,
        @PathVariable PayoutId payoutId) {

        var result =
            commandBus.execute(
                new ApprovePayoutCommand(
                    ctx.effectiveTenantIdRequired(),
                    payoutId,
                    ctx.userId()));

        return ApiResponse.success(mapper.toResponse(result));
    }

    @PostMapping("/{payoutId}/reject")
    @PreAuthorize("hasPermission(null, 'PAYOUT_REJECT')")
    public ApiResponse<PayoutWorkflowResponse> reject(
        @CurrentContext TchRequestContext ctx,
        @PathVariable PayoutId payoutId,
        @Valid @RequestBody RejectPayoutRequest body) {

        var result =
            commandBus.execute(
                new RejectPayoutCommand(
                    ctx.effectiveTenantIdRequired(),
                    payoutId,
                    ctx.userId(),
                    body.reason()));

        return ApiResponse.success(mapper.toResponse(result));
    }
}
