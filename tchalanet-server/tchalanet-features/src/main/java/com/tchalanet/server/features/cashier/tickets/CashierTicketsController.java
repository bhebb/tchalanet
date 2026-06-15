package com.tchalanet.server.features.cashier.tickets;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.common.web.paging.TchPaging;
import com.tchalanet.server.features.cashier.tickets.app.CashierTicketReceiptService;
import com.tchalanet.server.features.cashier.tickets.app.CashierTicketsService;
import com.tchalanet.server.features.cashier.tickets.model.CashierSellTicketRequest;
import com.tchalanet.server.features.cashier.tickets.model.CashierSellTicketResponse;
import com.tchalanet.server.features.cashier.tickets.model.CashierTicketCancelRequest;
import com.tchalanet.server.features.cashier.tickets.model.CashierTicketCancelResponse;
import com.tchalanet.server.features.cashier.tickets.model.CashierTicketDetailsResponse;
import com.tchalanet.server.features.cashier.tickets.model.CashierTicketPageResponse;
import com.tchalanet.server.features.cashier.tickets.model.CashierTicketPreviewRequest;
import com.tchalanet.server.features.cashier.tickets.model.CashierTicketPreviewResponse;
import com.tchalanet.server.features.cashier.tickets.model.CashierTicketVerificationResponse;
import com.tchalanet.server.features.cashier.tickets.model.CashierVerifyTicketRequest;
import com.tchalanet.server.features.cashier.tickets.model.PrintTicketRequest;
import com.tchalanet.server.features.cashier.tickets.model.SendTicketReceiptRequest;
import com.tchalanet.server.features.cashier.tickets.model.SendTicketReceiptResponse;
import com.tchalanet.server.platform.audit.api.AuditLog;
import com.tchalanet.server.platform.audit.api.model.AuditAction;
import com.tchalanet.server.platform.audit.api.model.AuditEntityType;
import com.tchalanet.server.platform.idempotence.api.RequireIdempotency;
import com.tchalanet.server.platform.idempotence.api.model.IdempotencyScope;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tenant/cashier/tickets")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ACTOR_SELLER_TERMINAL') or hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'SUPER_ADMIN')")
@Tag(name = "Cashier • Tickets")
public class CashierTicketsController {

    private final CashierTicketsService ticketsService;
    private final CashierTicketReceiptService receiptService;

    @PostMapping("/preview")
    @Operation(summary = "Preview a sale (read-only sale acceptance evaluation)")
    public ApiResponse<CashierTicketPreviewResponse> preview(
        @CurrentContext TchRequestContext ctx,
        @Valid @RequestBody CashierTicketPreviewRequest request
    ) {
        return ApiResponse.success(ticketsService.preview(ctx, request));
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify a scanned public ticket code or URL for POS payout readiness")
    public ApiResponse<CashierTicketVerificationResponse> verify(
        @CurrentContext TchRequestContext ctx,
        @Valid @RequestBody CashierVerifyTicketRequest request
    ) {
        return ApiResponse.success(ticketsService.verify(ctx, request));
    }

    @PostMapping("/sell")
    @ResponseStatus(HttpStatus.CREATED)
    @RequireIdempotency(scope = IdempotencyScope.SALES_SELL_TICKET)
    @Operation(summary = "Place a ticket sale (idempotent). Auditing is driven by TicketPlacedEvent downstream.")
    public ApiResponse<CashierSellTicketResponse> sell(
        @CurrentContext TchRequestContext ctx,
        @Valid @RequestBody CashierSellTicketRequest request
    ) {
        return ApiResponse.created(ticketsService.sell(ctx, request));
    }

    @PostMapping("/{ticketId}/cancel")
    @AuditLog(
        entity = AuditEntityType.TICKET,
        action = AuditAction.CANCEL_TICKET,
        idExpression = "#ticketId",
        detailsExpression = "#request")
    @Operation(summary = "Cancel a ticket within the cancel window")
    public ApiResponse<CashierTicketCancelResponse> cancel(
        @CurrentContext TchRequestContext ctx,
        @PathVariable TicketId ticketId,
        @Valid @RequestBody CashierTicketCancelRequest request
    ) {
        return ApiResponse.success(ticketsService.cancel(ctx, ticketId, request));
    }

    @GetMapping
    @Operation(summary = "List cashier tickets")
    public ApiResponse<TchPage<CashierTicketPageResponse>> list(
        @TchPaging(
            allowedSort = {"createdAt", "totalAmount", "ticketCode"},
            defaultSort = {"createdAt,desc"})
        TchPageRequest page
    ) {
        return ApiResponse.success(ticketsService.listTickets(page.pageable()));
    }

    @GetMapping("/{ticketId}")
    @Operation(summary = "Get ticket details")
    public ApiResponse<CashierTicketDetailsResponse> get(@PathVariable TicketId ticketId) {
        return ApiResponse.success(ticketsService.getDetails(ticketId));
    }

    @PostMapping("/{ticketId}/print")
    @AuditLog(
        entity = AuditEntityType.TICKET,
        action = AuditAction.PRINT_TICKET,
        idExpression = "#ticketId",
        detailsExpression = "#request")
    @Operation(summary = "Render a ticket for print or delivery (binary)")
    public ResponseEntity<ByteArrayResource> print(
        @CurrentContext TchRequestContext ctx,
        @PathVariable TicketId ticketId,
        @Valid @RequestBody PrintTicketRequest request
    ) {
        return receiptService.print(ctx, ticketId, request);
    }

    @PostMapping("/{ticketId}/send")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Send a ticket receipt through an external channel (text-only). "
        + "Auditing is driven by platform.communication delivery events.")
    public ApiResponse<SendTicketReceiptResponse> send(
        @CurrentContext TchRequestContext ctx,
        @PathVariable TicketId ticketId,
        @Valid @RequestBody SendTicketReceiptRequest request
    ) {
        return ApiResponse.accepted(receiptService.send(ctx, ticketId, request));
    }
}
