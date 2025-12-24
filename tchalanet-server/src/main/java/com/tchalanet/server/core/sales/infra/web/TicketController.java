package com.tchalanet.server.core.sales.infra.web;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.core.sales.application.command.model.*;
import com.tchalanet.server.core.sales.application.query.model.*;
import com.tchalanet.server.core.sales.domain.model.Ticket;
import com.tchalanet.server.core.sales.infra.web.mapper.TicketWebMapper;
import com.tchalanet.server.core.sales.infra.web.model.*;
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
  public ResponseEntity<SellTicketResponse> sell(@Valid @RequestBody SellTicketRequest request) {
    var cmd = mapper.toSellCommand(request);
    var result = commandBus.send(cmd);
    var response = mapper.toSellTicketResponse(result);
    if ("PENDING_APPROVAL".equals(result.status())) {
      return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    } else {
      return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
  }

  // --- LIST ---
  @GetMapping
  public ResponseEntity<PagedResponse<TicketSummaryResponse>> list(
      @RequestParam(required = false) UUID terminalId,
      @RequestParam(required = false) UUID drawId,
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
  public ResponseEntity<TicketResponse> details(@PathVariable UUID ticketId) {
    GetTicketDetailsQuery q = new GetTicketDetailsQuery(ticketId);
    var dto = queryBus.send(q);
    if (dto == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(mapper.toTicketResponse(dto));
  }

  // --- CANCEL (VOID) ---
  @PatchMapping("/{ticketId}/cancel")
  public ResponseEntity<CancelSaleResponse> cancel(
      @PathVariable UUID ticketId, @Valid @RequestBody CancelTicketRequest request) {
    CancelSaleCommand cmd = mapper.toCancelTicketCommand(ticketId, request);
    CancelSaleResult result = commandBus.send(cmd);
    CancelSaleResponse response = mapper.toCancelSaleResponse(result);
    return ResponseEntity.ok(response);
  }

  // --- PAYMENT PENDING ---
  @PatchMapping("/{ticketId}/payment-pending")
  public ResponseEntity<TicketResponse> paymentPending(
      @PathVariable UUID ticketId, @Valid @RequestBody MarkPaymentPendingRequest request) {
    MarkPaymentPendingCommand cmd = mapper.toMarkPaymentPendingCommand(ticketId, request);
    Ticket updated = commandBus.send(cmd);
    return ResponseEntity.ok(mapper.toTicketResponse(updated));
  }

  // --- PAID ---
  @PatchMapping("/{ticketId}/paid")
  public ResponseEntity<TicketResponse> paid(
      @PathVariable UUID ticketId, @Valid @RequestBody MarkPaidRequest request) {
    MarkTicketPaidCommand cmd = mapper.toMarkTicketPaidCommand(ticketId, request);
    Ticket updated = commandBus.send(cmd);
    return ResponseEntity.ok(mapper.toTicketResponse(updated));
  }

  // --- PRINT ---
  @GetMapping("/{ticketId}/print")
  public ResponseEntity<String> print(@PathVariable UUID ticketId) {
    var cmd = new PrintTicketCommand(ticketId);
    var printable = commandBus.send(cmd);
    return ResponseEntity.ok(printable);
  }
}
