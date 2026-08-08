package com.tchalanet.server.features.pos.tickets;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.common.web.paging.TchPaging;
import com.tchalanet.server.features.pos.tickets.app.PosTicketReceiptService;
import com.tchalanet.server.features.pos.tickets.app.PosTicketsService;
import com.tchalanet.server.features.pos.tickets.model.PosTicketCancelRequest;
import com.tchalanet.server.features.pos.tickets.model.PosTicketCancelResponse;
import com.tchalanet.server.features.pos.tickets.model.PosTicketDetailsResponse;
import com.tchalanet.server.features.pos.tickets.model.PosTicketPageResponse;
import com.tchalanet.server.features.pos.tickets.model.PosTicketVerificationResponse;
import com.tchalanet.server.features.pos.tickets.model.PosVerifyTicketRequest;
import com.tchalanet.server.features.pos.tickets.model.PrintTicketRequest;
import com.tchalanet.server.features.pos.tickets.model.SellerTerminalDailyStatsResponse;
import com.tchalanet.server.features.pos.tickets.model.SendTicketReceiptRequest;
import com.tchalanet.server.features.pos.tickets.model.SendTicketReceiptResponse;
import com.tchalanet.server.platform.audit.api.AuditLog;
import com.tchalanet.server.platform.audit.api.model.AuditAction;
import com.tchalanet.server.platform.audit.api.model.AuditEntityType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tenant/cashier/tickets")
@RequiredArgsConstructor
@PreAuthorize(
    "hasAuthority('ACTOR_SELLER_TERMINAL') or hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'SUPER_ADMIN')")
@Tag(name = "Cashier • Tickets")
public class PosTicketsController {

  private final PosTicketsService ticketsService;
  private final PosTicketReceiptService receiptService;

  @PostMapping("/verify")
  @Operation(summary = "Verify a scanned public ticket code or URL after settlement")
  public ApiResponse<PosTicketVerificationResponse> verify(
      @CurrentContext TchRequestContext ctx, @Valid @RequestBody PosVerifyTicketRequest request) {
    return ApiResponse.success(ticketsService.verify(ctx, request));
  }

  @PostMapping("/{ticketId}/cancel")
  @AuditLog(
      entity = AuditEntityType.TICKET,
      action = AuditAction.CANCEL_TICKET,
      idExpression = "#ticketId",
      detailsExpression = "#request")
  @Operation(summary = "Cancel a ticket within the cancel window")
  public ApiResponse<PosTicketCancelResponse> cancel(
      @CurrentContext TchRequestContext ctx,
      @PathVariable TicketId ticketId,
      @Valid @RequestBody PosTicketCancelRequest request) {
    return ApiResponse.success(ticketsService.cancel(ctx, ticketId, request));
  }

  @GetMapping("/stats")
  @Operation(
      summary =
          "Sales stats for the authenticated seller terminal. Defaults to today in tenant timezone.")
  public ApiResponse<SellerTerminalDailyStatsResponse> stats(
      @CurrentContext TchRequestContext ctx, @RequestParam(required = false) String date) {
    return ApiResponse.success(ticketsService.sellerTerminalStats(ctx, date));
  }

  @GetMapping
  @Operation(summary = "List cashier tickets")
  public ApiResponse<TchPage<PosTicketPageResponse>> list(
      @CurrentContext TchRequestContext ctx,
      @RequestParam(required = false) SellerTerminalId sellerTerminalId,
      @RequestParam(required = false) DrawId drawId,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String resultStatus,
      @RequestParam(required = false) String settlementStatus,
      @RequestParam(required = false) String provider,
      @RequestParam(required = false) Boolean winningOnly,
      @RequestParam(required = false) String q,
      @RequestParam(required = false) Instant from,
      @RequestParam(required = false) Instant to,
      @RequestParam(required = false) LocalDate fromDate,
      @RequestParam(required = false) LocalDate toDate,
      @TchPaging(
              allowedSort = {"createdAt", "totalAmount", "ticketCode"},
              defaultSort = {"createdAt,desc"})
          TchPageRequest page) {
    Instant effectiveFrom = from != null ? from : startOfDay(fromDate, ctx);
    Instant effectiveTo = to != null ? to : endOfDay(toDate, ctx);
    return ApiResponse.success(
        ticketsService.listTickets(
            ctx,
            sellerTerminalId,
            drawId,
            status,
            resultStatus,
            settlementStatus,
            provider,
            winningOnly,
            q,
            effectiveFrom,
            effectiveTo,
            page.pageable()));
  }

  @GetMapping("/{ticketId}")
  @Operation(summary = "Get ticket details")
  public ApiResponse<PosTicketDetailsResponse> get(@PathVariable TicketId ticketId) {
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
      @Valid @RequestBody PrintTicketRequest request) {
    return receiptService.print(ctx, ticketId, request);
  }

  @PostMapping("/{ticketId}/send")
  @ResponseStatus(HttpStatus.ACCEPTED)
  @Operation(
      summary =
          "Send a ticket receipt through an external channel (text-only). "
              + "Auditing is driven by platform.communication delivery events.")
  public ApiResponse<SendTicketReceiptResponse> send(
      @CurrentContext TchRequestContext ctx,
      @PathVariable TicketId ticketId,
      @Valid @RequestBody SendTicketReceiptRequest request) {
    return ApiResponse.accepted(receiptService.send(ctx, ticketId, request));
  }

  private static Instant startOfDay(LocalDate date, TchRequestContext ctx) {
    return date == null ? null : date.atStartOfDay(tenantZone(ctx)).toInstant();
  }

  private static Instant endOfDay(LocalDate date, TchRequestContext ctx) {
    return date == null
        ? null
        : date.plusDays(1).atStartOfDay(tenantZone(ctx)).minusNanos(1).toInstant();
  }

  private static ZoneId tenantZone(TchRequestContext ctx) {
    return ctx != null && ctx.tenantZoneId() != null ? ctx.tenantZoneId() : ZoneOffset.UTC;
  }
}
