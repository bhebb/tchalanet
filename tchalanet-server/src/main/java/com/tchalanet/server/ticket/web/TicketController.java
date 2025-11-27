package com.tchalanet.server.ticket.web;

import com.tchalanet.server.ticket.domain.ports.in.*;
import com.tchalanet.server.ticket.web.dto.*;
import com.tchalanet.server.ticket.web.mapper.TicketWebMapper;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/tickets")
@RequiredArgsConstructor
public class TicketController {

  private final CreateTicketUseCase createTicketUseCase;
  private final ListTicketsQuery listTicketsQuery;
  private final GetTicketDetailsQuery getTicketDetailsQuery;
  private final UpdateTicketStatusUseCase updateTicketStatusUseCase;
  private final PrintTicketUseCase printTicketUseCase;
  private final ArchiveTicketsUseCase archiveTicketsUseCase; // For batch job, not direct API

  private final TicketWebMapper mapper;

  // --- CREATE ---
  @PostMapping
  public ResponseEntity<TicketResponse> createTicket(
      @PathVariable UUID tenantId, @Valid @RequestBody CreateTicketRequest request) {
    CreateTicketUseCase.CreateTicketCommand command = mapper.toCreateCommand(request, tenantId);
    var ticket = createTicketUseCase.createTicket(command);
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
            status != null
                ? com.tchalanet.server.ticket.domain.model.TicketStatus.valueOf(
                    status.toUpperCase())
                : null,
            from,
            to);
    ListTicketsQuery.PageRequest pageRequest = new ListTicketsQuery.PageRequest(page, size);
    var pagedResult = listTicketsQuery.search(filter, pageRequest);
    return ResponseEntity.ok(mapper.toPagedSummaryResponse(pagedResult));
  }

  // --- READ (Details by ID) ---
  @GetMapping("/{ticketId}")
  public ResponseEntity<TicketResponse> getTicketDetailsById(
      @PathVariable UUID tenantId, @PathVariable UUID ticketId) {
    var dto =
        getTicketDetailsQuery
            .findById(ticketId)
            .filter(details -> details.tenantId().equals(tenantId)) // Security check
            .orElseThrow(() -> new TicketNotFoundException("Ticket not found: " + ticketId));
    return ResponseEntity.ok(mapper.toTicketResponse(dto));
  }

  // --- READ (Details by Public Code) ---
  @GetMapping("/public/{publicCode}")
  public ResponseEntity<TicketResponse> getTicketDetailsByPublicCode(
      @PathVariable UUID tenantId, // TenantId is still required for security/scoping
      @PathVariable String publicCode) {
    var dto =
        getTicketDetailsQuery
            .findByPublicCode(publicCode)
            .filter(details -> details.tenantId().equals(tenantId)) // Security check
            .orElseThrow(
                () ->
                    new TicketNotFoundException(
                        "Ticket not found with public code: " + publicCode));
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
    var updatedTicket = updateTicketStatusUseCase.markAsPaid(command);
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
    var updatedTicket = updateTicketStatusUseCase.voidTicket(command);
    return ResponseEntity.ok(mapper.toTicketResponse(updatedTicket));
  }

  // --- PRINT ---
  @GetMapping("/{ticketId}/print")
  public ResponseEntity<String> printTicket(
      @PathVariable UUID tenantId, @PathVariable UUID ticketId) {
    String printableContent = printTicketUseCase.getPrintableTicket(ticketId, tenantId);
    return ResponseEntity.ok(printableContent);
  }

  // --- ARCHIVE (Typically not a direct API endpoint, but for completeness) ---
  // This would usually be triggered by a scheduled job or an admin tool, not a direct user API.
  // @PostMapping("/archive")
  // public ResponseEntity<Integer> archiveOldTickets(
  //     @PathVariable UUID tenantId,
  //     @RequestParam @NotNull Instant cutoffDate
  // ) {
  //     int archivedCount = archiveTicketsUseCase.archiveTickets(tenantId, cutoffDate);
  //     return ResponseEntity.ok(archivedCount);
  // }

  // --- Exception Handling ---
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public static class TicketNotFoundException extends RuntimeException {
    public TicketNotFoundException(String message) {
      super(message);
    }
  }
}
