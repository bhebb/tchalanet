package com.tchalanet.server.core.sales.internal.infra.web;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.types.enums.IdempotencyScope;
import com.tchalanet.server.core.sales.internal.infra.web.mapper.TicketWebMapper;
import com.tchalanet.server.core.sales.internal.infra.web.model.SellTicketRequest;
import com.tchalanet.server.core.sales.internal.infra.web.model.TicketResponse;
import com.tchalanet.server.platform.idempotence.api.RequireIdempotency;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tenant/tickets")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('CASHIER', 'TENANT_ADMIN', 'SUPER_ADMIN')")
public class TicketSalesController {

    private final CommandBus commandBus;
    private final TicketWebMapper mapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasPermission(null, 'TICKET_SELL')")
    @RequireIdempotency(scope = IdempotencyScope.SALES_SELL_TICKET)
    public ApiResponse<TicketResponse> sell(
        @CurrentContext TchRequestContext ctx,
        @Valid @RequestBody SellTicketRequest request) {
        var result = commandBus.execute(mapper.toSellCommand(ctx, request));
        return ApiResponse.created(mapper.toTicketResponse(result.ticket()));
    }
}
