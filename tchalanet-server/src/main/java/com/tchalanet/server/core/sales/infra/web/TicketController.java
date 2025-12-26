package com.tchalanet.server.core.sales.infra.web;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TerminalId;

import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.core.sales.application.command.model.*;
import com.tchalanet.server.core.sales.application.query.model.*;
import com.tchalanet.server.core.sales.domain.model.Ticket;
import com.tchalanet.server.core.sales.infra.web.mapper.TicketWebMapper;
import com.tchalanet.server.core.sales.infra.web.model.*;
import com.tchalanet.server.common.web.api.ApiResponse;
import jakarta.validation.Valid;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final CommandBus commandBus;
    private final QueryBus queryBus;
    private final TicketWebMapper mapper;

    // --- SELL ---
    @PostMapping
    public ResponseEntity<TicketResponse> sell(@Valid @RequestBody SellTicketRequest request) {
        var cmd = mapper.toSellCommand(request);
        var result = commandBus.send(cmd);
        if ("PENDING_APPROVAL".equals(result.status())) {
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(null); // Body will be wrapped by advice
        } else {
            return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toTicketResponse(result.ticket()));
        }
    }

    // --- LIST ---
    @GetMapping
    public ResponseEntity<PagedResponse<TicketSummaryResponse>> list(
        @RequestParam(required = false) TerminalId terminalId,
        @RequestParam(required = false) DrawId drawId,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) java.time.Instant from,
        @RequestParam(required = false) java.time.Instant to,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
        ListTicketsQuery q = mapper.toListTicketsQuery(terminalId, drawId, status, from, to, page, size);
        var result = queryBus.send(q);
        return ResponseEntity.ok(mapper.toPagedSummaryResponse(result));
    }

    // --- DETAILS ---
    @GetMapping("/{ticketId}")
    public ResponseEntity<TicketResponse> details(@PathVariable TicketId ticketId) {
        GetTicketDetailsQuery q = new GetTicketDetailsQuery(ticketId);
        var dto = queryBus.send(q);
        if (dto == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(mapper.toTicketResponse(dto));
    }

    @PatchMapping("/{ticketId}/cancel")
    public ResponseEntity<CancelSaleResponse> cancel(
        @PathVariable TicketId ticketId, @Valid @RequestBody CancelTicketRequest request) {
        var cmd = mapper.toCancelTicketCommand(ticketId, request);
        var result = commandBus.send(cmd);
        var response = mapper.toCancelSaleResponse(result);
        return ResponseEntity.ok(response);
    }

    // --- PAYMENT PENDING ---
    @PatchMapping("/{ticketId}/payment-pending")
    public ResponseEntity<TicketResponse> paymentPending(
        @PathVariable TicketId ticketId, @Valid @RequestBody MarkPaymentPendingRequest request) {
        var cmd = mapper.toMarkPaymentPendingCommand(ticketId, request);
        var updated = commandBus.send(cmd);
        return ResponseEntity.ok(mapper.toTicketResponse(updated));
    }

    // --- PAID ---
    @PatchMapping("/{ticketId}/paid")
    public ResponseEntity<TicketResponse> paid(
        @PathVariable TicketId ticketId, @Valid @RequestBody MarkPaidRequest request) {
        var cmd = mapper.toMarkTicketPaidCommand(ticketId, request);
        var updated = commandBus.send(cmd);
        return ResponseEntity.ok(mapper.toTicketResponse(updated));
    }

    // --- PRINT ---
    @GetMapping("/{ticketId}/print")
    public ResponseEntity<String> print(@PathVariable TicketId ticketId) {
        var cmd = new PrintTicketCommand(ticketId);
        var printable = commandBus.send(cmd);
        return ResponseEntity.ok(printable);
    }
}
