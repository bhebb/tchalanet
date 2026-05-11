package com.tchalanet.server.core.payout.infra.web;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.context.CurrentContext;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.types.id.PayoutId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.core.payout.application.command.model.ExecutePayoutCommand;
import com.tchalanet.server.core.payout.application.command.model.RegisterPayoutCommand;
import com.tchalanet.server.core.payout.infra.web.mapper.PayoutWebMapper;
import com.tchalanet.server.core.payout.infra.web.model.ExecutePayoutRequest;
import com.tchalanet.server.core.payout.infra.web.model.PayoutWorkflowResponse;
import com.tchalanet.server.core.payout.infra.web.model.RegisterPayoutRequest;
import com.tchalanet.server.core.payout.infra.web.model.RegisterPayoutResponse;
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
@PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'SUPER_ADMIN')")
@Tags({@Tag(name = "Payouts • Tenant")})
@Validated
public class PayoutTenantController {

    private final CommandBus commandBus;
    private final PayoutWebMapper mapper;

    @PostMapping
    @PreAuthorize("hasPermission('payout.request')")
    public ApiResponse<RegisterPayoutResponse> request(
        @CurrentContext TchRequestContext ctx,
        @Valid @RequestBody RegisterPayoutRequest body) {

        var result =
            commandBus.execute(
                new RegisterPayoutCommand(
                    ctx.effectiveTenantIdRequired(),
                    body.ticketId(),
                    ctx.userId(),

                    body.payingSessionId(),
                    body.payingOutletId(),

                    body.terminalId(),
                    body.reason()));

        return ApiResponse.success(mapper.toResponse(result));
    }

    @PostMapping("/{payoutId}/execute")
    @PreAuthorize("hasPermission('payout.register')")
    public ApiResponse<PayoutWorkflowResponse> execute(
        @CurrentContext TchRequestContext ctx,
        @PathVariable PayoutId payoutId,
        @Valid @RequestBody ExecutePayoutRequest body) {

        var result =
            commandBus.execute(
                new ExecutePayoutCommand(
                    ctx.effectiveTenantIdRequired(),
                    payoutId,
                    ctx.userId(),
                    body.payingSessionId(),
                    body.payingOutletId(),
                    body.terminalId(),
                    body.reason()));

        return ApiResponse.success(mapper.toResponse(result));
    }
}
