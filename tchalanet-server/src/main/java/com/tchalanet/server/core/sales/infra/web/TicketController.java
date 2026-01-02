package com.tchalanet.server.core.sales.infra.web;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.sales.application.command.model.*;
import com.tchalanet.server.core.sales.application.query.model.*;
import com.tchalanet.server.core.sales.infra.web.mapper.TicketWebMapper;
import com.tchalanet.server.core.sales.infra.web.model.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tenant/tickets")
@RequiredArgsConstructor
@Tag(name = "Tenant • Tickets")
public class TicketController {

  private final CommandBus commandBus;
  private final QueryBus queryBus;
  private final TicketWebMapper mapper;

  // --- SELL ---
  @Operation(summary = "Sell a ticket (tenant)")
  @PostMapping
  public ResponseEntity<TicketResponse> sell(@Valid @RequestBody SellTicketRequest request) {
    var cmd = mapper.toSellCommand(request);
    var result = commandBus.send(cmd);
    if ("PENDING_APPROVAL".equals(result.status())) {
      return ResponseEntity.status(HttpStatus.ACCEPTED)
          .body(null); // Body will be wrapped by advice
    } else {
      return ResponseEntity.status(HttpStatus.CREATED)
          .body(mapper.toTicketResponse(result.ticket()));
    }
  }

  // --- LIST ---
  @Operation(summary = "List tickets for tenant with filters")
  @GetMapping
  public ResponseEntity<PagedResponse<TicketSummaryResponse>> list(
      @RequestParam(required = false) TerminalId terminalId,
      @RequestParam(required = false) DrawId drawId,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) java.time.Instant from,
      @RequestParam(required = false) java.time.Instant to,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    ListTicketsQuery q =
        mapper.toListTicketsQuery(terminalId, drawId, status, from, to, page, size);
    var result = queryBus.send(q);
    return ResponseEntity.ok(mapper.toPagedSummaryResponse(result));
  }

  // --- DETAILS ---
  @Operation(summary = "Get ticket details (tenant)")
  @GetMapping("/{ticketId}")
  public ResponseEntity<TicketResponse> details(@PathVariable TicketId ticketId) {
    GetTicketDetailsQuery q = new GetTicketDetailsQuery(ticketId);
    var dto = queryBus.send(q);
    if (dto == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(mapper.toTicketResponse(dto));
  }

  @Operation(summary = "Cancel a ticket (tenant)")
  @PatchMapping("/{ticketId}/cancel")
  public ResponseEntity<CancelSaleResponse> cancel(
      @PathVariable TicketId ticketId, @Valid @RequestBody CancelTicketRequest request) {
    var cmd = mapper.toCancelTicketCommand(ticketId, request);
    var result = commandBus.send(cmd);
    var response = mapper.toCancelSaleResponse(result);
    return ResponseEntity.ok(response);
  }

  // --- PAYMENT PENDING ---
  @Operation(summary = "Mark ticket payment pending (tenant)")
  @PatchMapping("/{ticketId}/payment-pending")
  public ResponseEntity<TicketResponse> paymentPending(
      @PathVariable TicketId ticketId, @Valid @RequestBody MarkPaymentPendingRequest request) {
    var cmd = mapper.toMarkPaymentPendingCommand(ticketId, request);
    var updated = commandBus.send(cmd);
    return ResponseEntity.ok(mapper.toTicketResponse(updated));
  }

  // --- PAID ---
  @Operation(summary = "Mark ticket paid (tenant)")
  @PatchMapping("/{ticketId}/paid")
  public ResponseEntity<TicketResponse> paid(
      @PathVariable TicketId ticketId, @Valid @RequestBody MarkPaidRequest request) {
    var cmd = mapper.toMarkTicketPaidCommand(ticketId, request);
    var updated = commandBus.send(cmd);
    return ResponseEntity.ok(mapper.toTicketResponse(updated));
  }

  // --- PRINT ---
  @Operation(summary = "Get printable ticket content (tenant)")
  @GetMapping("/{ticketId}/print")
  public ResponseEntity<String> print(@PathVariable TicketId ticketId) {
    var cmd = new PrintTicketCommand(ticketId);
    var printable = commandBus.send(cmd);
    return ResponseEntity.ok(printable);
  }
}
