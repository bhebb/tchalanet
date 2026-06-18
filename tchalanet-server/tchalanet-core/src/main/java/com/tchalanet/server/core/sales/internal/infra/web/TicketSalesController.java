package com.tchalanet.server.core.sales.internal.infra.web;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.web.advice.ApiResponseContext;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketCommand;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketLineInput;
import com.tchalanet.server.core.sales.internal.infra.web.mapper.TicketWebMapper;
import com.tchalanet.server.core.sales.internal.infra.web.model.SellTicketLineRequest;
import com.tchalanet.server.core.sales.internal.infra.web.model.SellTicketRequest;
import com.tchalanet.server.core.sales.internal.infra.web.model.SellTicketResponse;
import com.tchalanet.server.platform.idempotence.api.RequireIdempotency;
import com.tchalanet.server.platform.idempotence.api.model.IdempotencyScope;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/tenant/tickets")
@RequiredArgsConstructor
@Tag(name = "Sales • Sell", description = "Endpoints for ticket sale operations")
@PreAuthorize("hasPermission('seller_terminal.sell')")
public class TicketSalesController {

    private final CommandBus commandBus;
    private final TicketWebMapper mapper;

    @Operation(
        operationId = "sellTicket",
        summary = "Sell a ticket",
        description = "Creates a ticket for the current tenant and returns the sale payload.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Ticket sold"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Idempotency conflict")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequireIdempotency(scope = IdempotencyScope.SALES_SELL_TICKET)
    public ApiResponse<SellTicketResponse> sell(
        @CurrentContext TchRequestContext ctx,
        @Valid @RequestBody SellTicketRequest body
    ) {
        ctx.sellerTerminalIdRequired();

        var result = commandBus.execute(new SellTicketCommand(
            body.drawId(), body.drawChannelId(), body.currency(),
            toLines(body.lines()), body.serviceOptions(), List.of()));

        var responseCtx = ApiResponseContext.get();
        result.notices().forEach(responseCtx::addNotice);

        return ApiResponse.success(mapper.toSellResponse(result));
    }

    private List<SellTicketLineInput> toLines(@NotEmpty @Valid List<SellTicketLineRequest> lines) {
        return lines.stream().map(SellTicketLineRequest::toLine).toList();
    }
}
