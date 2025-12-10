package com.tchalanet.server.core.sales.infra.web;

import com.tchalanet.server.core.sales.domain.model.TicketStatus;
import com.tchalanet.server.core.sales.application.command.handler.CreateTicketCommandHandler;
import com.tchalanet.server.core.sales.application.query.handler.ListTicketsQueryHandler;
import com.tchalanet.server.core.sales.application.query.handler.GetTicketDetailsQueryHandler;
import com.tchalanet.server.core.sales.application.command.handler.UpdateTicketStatusCommandHandler;
import com.tchalanet.server.core.sales.application.command.handler.PrintTicketCommandHandler;
import com.tchalanet.server.core.sales.application.command.model.CreateTicketCommand;
import com.tchalanet.server.core.sales.application.query.model.ListTicketsQuery;
import com.tchalanet.server.core.sales.application.query.model.ListTicketsQuery.TicketSummaryDto;
import com.tchalanet.server.core.sales.application.query.model.ListTicketsQuery.PagedResult;
import com.tchalanet.server.core.sales.application.query.model.GetTicketDetailsByIdQuery;
import com.tchalanet.server.core.sales.application.port.in.UpdateTicketStatusUseCase;
import com.tchalanet.server.core.sales.infra.web.model.*;
import com.tchalanet.server.core.sales.infra.web.mapper.TicketWebMapper;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/tickets")
@RequiredArgsConstructor
public class TicketController {

  private final CreateTicketCommandHandler createTicketHandler;
  private final ListTicketsQueryHandler listTicketsHandler;
  private final GetTicketDetailsQueryHandler getTicketDetailsHandler;
  private final UpdateTicketStatusCommandHandler updateTicketStatusHandler;
  private final PrintTicketCommandHandler printTicketHandler;

  private final TicketWebMapper mapper;

  // --- CREATE ---
  @PostMapping
  public ResponseEntity<TicketResponse> createTicket(
      @PathVariable UUID tenantId, @Valid @RequestBody CreateTicketRequest request) {
    CreateTicketCommand command = mapper.toCreateCommand(request, tenantId);
    var ticket = createTicketHandler.handle(command);
    return new ResponseEntity<>(mapper.toTicketResponse(ticket), HttpStatus.CREATED);
  }

  // --- READ (List) ---
  @GetMapping
  public ResponseEntity<PagedResponse<TicketSummaryResponse>> listTickets(
      @PathVariable UUID tenantId,
      @RequestParam(required = false) UUID terminalId,
      @RequestParam(required = false) UUID drawId,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Instant from,
      @RequestParam(required = false) Instant to,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    ListTicketsQuery.TicketFilter filter =
        new ListTicketsQuery.TicketFilter(
            tenantId,
            terminalId,
            drawId,
            status != null ? TicketStatus.valueOf(status.toUpperCase()) : null,
            from,
            to);
    ListTicketsQuery.PageRequest pageRequest = new ListTicketsQuery.PageRequest(page, size);

    ListTicketsQuery query = new ListTicketsQuery(filter, pageRequest);

    var pagedResult = listTicketsHandler.handle(query);
    return ResponseEntity.ok(mapper.toPagedSummaryResponse(pagedResult));
  }

  // --- READ (Details by ID) ---
  @GetMapping("/{ticketId}")
  public ResponseEntity<TicketResponse> getTicketDetailsById(
      @PathVariable UUID tenantId, @PathVariable UUID ticketId) {
    Optional<GetTicketDetailsByIdQuery.TicketDetailsDto> dtoOpt = getTicketDetailsHandler.findById(ticketId);
    if (dtoOpt.isEmpty()) {
      throw new TicketNotFoundException("Ticket not found: " + ticketId);
    }
    GetTicketDetailsByIdQuery.TicketDetailsDto dto = dtoOpt.get();
    if (!tenantId.equals(dto.tenantId())) {
      throw new TicketNotFoundException("Ticket not found: " + ticketId);
    }
    return ResponseEntity.ok(mapper.toTicketResponse(dto));
  }

  // --- READ (Details by Public Code) ---
  @GetMapping("/public/{publicCode}")
  public ResponseEntity<TicketResponse> getTicketDetailsByPublicCode(
      @PathVariable UUID tenantId, // TenantId is still required for security/scoping
      @PathVariable String publicCode) {
    Optional<GetTicketDetailsByIdQuery.TicketDetailsDto> dtoOpt = getTicketDetailsHandler.findByPublicCode(publicCode);
    if (dtoOpt.isEmpty()) {
      throw new TicketNotFoundException("Ticket not found with public code: " + publicCode);
    }
    GetTicketDetailsByIdQuery.TicketDetailsDto dto = dtoOpt.get();
    if (!tenantId.equals(dto.tenantId())) {
      throw new TicketNotFoundException("Ticket not found with public code: " + publicCode);
    }
    return ResponseEntity.ok(mapper.toTicketResponse(dto));
  }

  // --- UPDATE STATUS (Mark as Paid) ---
  @PatchMapping("/{ticketId}/paid")
  public ResponseEntity<TicketResponse> markTicketAsPaid(
      @PathVariable UUID tenantId,
      @PathVariable UUID ticketId,
      @Valid @RequestBody UpdateTicketStatusRequest request) {
    UpdateTicketStatusUseCase.UpdateStatusCommand command =
        new UpdateTicketStatusUseCase.UpdateStatusCommand(
            tenantId, ticketId, request.userId(), request.isAdmin());
    var updatedTicket = updateTicketStatusHandler.markAsPaid(command);
    return ResponseEntity.ok(mapper.toTicketResponse(updatedTicket));
  }

  // --- UPDATE STATUS (Void Ticket) ---
  @PatchMapping("/{ticketId}/void")
  public ResponseEntity<TicketResponse> voidTicket(
      @PathVariable UUID tenantId,
      @PathVariable UUID ticketId,
      @Valid @RequestBody UpdateTicketStatusRequest request) {
    UpdateTicketStatusUseCase.UpdateStatusCommand command =
        new UpdateTicketStatusUseCase.UpdateStatusCommand(
            tenantId, ticketId, request.userId(), request.isAdmin());
    var updatedTicket = updateTicketStatusHandler.voidTicket(command);
    return ResponseEntity.ok(mapper.toTicketResponse(updatedTicket));
  }

  // --- PRINT ---
  @GetMapping("/{ticketId}/print")
  public ResponseEntity<String> printTicket(
      @PathVariable UUID tenantId, @PathVariable UUID ticketId) {
    String printableContent = printTicketHandler.getPrintableTicket(ticketId, tenantId);
    return ResponseEntity.ok(printableContent);
  }

  // --- Exception Handling ---
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public static class TicketNotFoundException extends RuntimeException {
    public TicketNotFoundException(String message) {
      super(message);
    }
  }
}
