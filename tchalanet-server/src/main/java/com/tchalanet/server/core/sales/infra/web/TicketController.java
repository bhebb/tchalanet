package com.tchalanet.server.core.sales.infra.web;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.id.AgentId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.api.ApiNotice;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.api.NoticeSeverity;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.common.web.paging.TchPaging;
import com.tchalanet.server.core.sales.application.command.model.ApproveTicketSaleCommand;
import com.tchalanet.server.core.sales.application.command.model.RejectTicketSaleCommand;
import com.tchalanet.server.core.sales.application.command.model.SellTicketOutcome;
import com.tchalanet.server.core.sales.application.query.model.GetTicketDetailsQuery;
import com.tchalanet.server.core.sales.application.query.model.GetTicketPrintEscPosQuery;
import com.tchalanet.server.core.sales.application.query.model.GetTicketPrintPdfQuery;
import com.tchalanet.server.core.sales.application.query.model.ListTicketsQuery;
import com.tchalanet.server.core.sales.infra.web.mapper.TicketWebMapper;
import com.tchalanet.server.core.sales.infra.web.model.CancelSaleResponse;
import com.tchalanet.server.core.sales.infra.web.model.CancelTicketRequest;
import com.tchalanet.server.core.sales.infra.web.model.SellTicketRequest;
import com.tchalanet.server.core.sales.infra.web.model.TicketResponse;
import com.tchalanet.server.core.sales.infra.web.model.TicketSummaryResponse;
import com.tchalanet.server.core.sales.infra.web.model.OverrideTicketResultRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Base64;
import java.util.Map;

@RestController
@RequestMapping("/tenant/tickets")
@RequiredArgsConstructor
@Tag(name = "Tenant • Tickets")
public class TicketController {

    private final CommandBus commandBus;
    private final QueryBus queryBus;
    private final TicketWebMapper mapper;

    @Operation(summary = "Sell a ticket (tenant)")
    @PostMapping
    @Secured({"ROLE_CASHIER", "ROLE_ADMIN", "ROLE_SUPER_ADMIN"})
    public ResponseEntity<ApiResponse<TicketResponse>> sell(@Valid @RequestBody SellTicketRequest request) {
        var cmd = mapper.toSellCommand(request);
        var result = commandBus.send(cmd);

        if (SellTicketOutcome.PENDING_APPROVAL == result.outcome()) {
            var notice =
                new ApiNotice(
                    "APPROVAL_REQUIRED",
                    "Transaction requires approval",
                    "sales",
                    NoticeSeverity.WARN,
                    Map.of("approvalRequestId", result.approvalRequestId()));
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.pending(notice, mapper.toTicketResponse(result.ticket())));
        }

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.created(mapper.toTicketResponse(result.ticket())));
    }

    @Operation(summary = "Approve a pending ticket sale (tenant)")
    @PostMapping("/{ticketId}/approve")
    @Secured({"ROLE_ADMIN", "ROLE_SUPER_ADMIN"})
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<TicketResponse> approve(
        @PathVariable TicketId ticketId,
        @RequestParam UserId approvedBy,
        @RequestParam(required = false) String reason) {

        var cmd = new ApproveTicketSaleCommand(
            ticketId,
            approvedBy,
            reason
        );

        var res = commandBus.send(cmd);
        return ApiResponse.success(mapper.toTicketResponse(res.ticket()));
    }

    @Operation(summary = "Reject a pending ticket sale (tenant)")
    @PostMapping("/{ticketId}/reject")
    @Secured({"ROLE_ADMIN", "ROLE_SUPER_ADMIN"})
    @ResponseStatus(HttpStatus.OK)

    public ApiResponse<TicketResponse> reject(
        @PathVariable TicketId ticketId,
        @RequestParam UserId rejectedBy,
        @RequestParam(required = false) String reason) {

        var cmd = new RejectTicketSaleCommand(
            ticketId,
            rejectedBy,
            reason
        );

        var res = commandBus.send(cmd);
        return ApiResponse.success(mapper.toTicketResponse(res.ticket()));
    }

    /**
     * List tickets for tenant with filters and pagination.
     *
     * PAGINATION: Default 20, max 100 (offset-based via @TchPaging).
     * AUDIT: Automatically logged by query handler (v1 decision: list operations are audited).
     * RATE LIMITING: Not applied to tenant-scoped endpoints (v1 decision).
     *
     * @param terminalId Filter by terminal (optional)
     * @param outletId Filter by outlet (optional)
     * @param agentId Filter by agent (optional)
     * @param drawId Filter by draw (optional)
     * @param status Filter by status (optional)
     * @param from Filter by creation date from (optional, inclusive)
     * @param to Filter by creation date to (optional, inclusive)
     * @param pageReq Pagination parameters
     * @return Page of ticket summaries
     * @throws ResponseStatusException 400 if date range invalid (to < from)
     */
    @Operation(summary = "List tickets for tenant with filters")
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @Secured({"ROLE_CASHIER", "ROLE_ADMIN", "ROLE_SUPER_ADMIN"})
    public ApiResponse<TchPage<TicketSummaryResponse>> list(
        @RequestParam(required = false) TerminalId terminalId,
        @RequestParam(required = false) OutletId outletId,
        @RequestParam(required = false) AgentId agentId,
        @RequestParam(required = false) DrawId drawId,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) Instant from,
        @RequestParam(required = false) Instant to,
        @TchPaging(allowedSort = {"createdAt", "totalAmount", "ticketCode"},
            defaultSort = {"createdAt,DESC"}) TchPageRequest pageReq) {

        // Validate date range: created_to must be >= created_from
        if (from != null && to != null && to.isBefore(from)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "created_to must be greater than or equal to created_from"
            );
        }

        int page = pageReq.pageable().getPageNumber();
        int size = pageReq.pageable().getPageSize();
        ListTicketsQuery q =
            mapper.toListTicketsQuery(terminalId, drawId, status, from, to, page, size);
        var result = queryBus.send(q);
        return ApiResponse.success(mapper.toPagedSummaryResponse(result));
    }

    /**
     * Get ticket details by ID.
     *
     * AUDIT: Not logged (v1 decision: read-one is low risk, reduces log volume).
     * RATE LIMITING: Not applied to tenant-scoped endpoints (v1 decision).
     *
     * @param ticketId Ticket ID (typed ID)
     * @return Full ticket detail including lines and settlement info
     * @throws ResponseStatusException 404 if ticket not found
     */
    @Operation(summary = "Get ticket details (tenant)")
    @GetMapping("/{ticketId}")
    @ResponseStatus(HttpStatus.OK)
    @Secured({"ROLE_CASHIER", "ROLE_ADMIN", "ROLE_SUPER_ADMIN"})
    public ApiResponse<TicketResponse> details(@PathVariable TicketId ticketId) {
        GetTicketDetailsQuery q = new GetTicketDetailsQuery(ticketId);
        var dto = queryBus.send(q);
        if (dto == null) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Ticket not found with id: " + ticketId
            );
        }
        return ApiResponse.success(mapper.toTicketResponse(dto));
    }

    @Operation(summary = "Cancel a ticket (tenant)")
    @PatchMapping("/{ticketId}/cancel")
    @Secured({"ROLE_CASHIER", "ROLE_ADMIN", "ROLE_SUPER_ADMIN"})
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<CancelSaleResponse> cancel(
        @PathVariable TicketId ticketId, @Valid @RequestBody CancelTicketRequest request) {
        var cmd = mapper.toCancelSaleCommand(ticketId, request);
        var result = commandBus.send(cmd);
        var response = mapper.toCancelSaleResponse(result);
        return ApiResponse.success(response);
    }

    // --- PRINT ---
    @Operation(summary = "Get printable ticket content (tenant)")
    @GetMapping(path = "/{ticketId}/print")
    @Secured({"ROLE_CASHIER", "ROLE_ADMIN", "ROLE_SUPER_ADMIN"})
    public ResponseEntity<String> print(@PathVariable TicketId ticketId, HttpServletResponse res) {
        res.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");

        // reuse the existing PDF printer via QueryBus so behavior matches /print.pdf
        byte[] pdf = queryBus.send(new GetTicketPrintPdfQuery(ticketId));
        String asBase64 = Base64.getEncoder().encodeToString(pdf);
        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(asBase64);
    }

    // --- PRINT ESC/POS ---
    @Operation(summary = "Get ESC/POS printable bytes for a ticket (tenant)")
    @GetMapping(value = "/{ticketId}/print.escpos", produces = "application/octet-stream")
    @Secured({"ROLE_CASHIER", "ROLE_ADMIN", "ROLE_SUPER_ADMIN"})
    public byte[] printEscpos(@PathVariable TicketId ticketId, HttpServletResponse res) {
        res.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        res.setHeader(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=ticket-" + ticketId + ".bin");
        return queryBus.send(new GetTicketPrintEscPosQuery(ticketId));
    }

    // --- PRINT PDF ---
    @Operation(summary = "Get PDF printable for a ticket (tenant)")
    @GetMapping(value = "/{ticketId}/print.pdf", produces = "application/pdf")
    @Secured({"ROLE_CASHIER", "ROLE_ADMIN", "ROLE_SUPER_ADMIN"})
    public byte[] printPdf(@PathVariable TicketId ticketId, HttpServletResponse res) {
        res.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        res.setHeader(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=ticket-" + ticketId + ".pdf");
        return queryBus.send(new GetTicketPrintPdfQuery(ticketId));
    }

    // Admin: override ticket result
    @Operation(summary = "Override a ticket result (admin)")
    @PatchMapping("/{ticketId}/result/override")
    @Secured({"ROLE_ADMIN", "ROLE_SUPER_ADMIN"})
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> overrideResult(
        @PathVariable TicketId ticketId, @Valid @RequestBody OverrideTicketResultRequest request) {

        var cmd = mapper.toOverrideTicketResultCommand(ticketId, request);

        commandBus.send(cmd);
        return ApiResponse.success(null);
    }
}
